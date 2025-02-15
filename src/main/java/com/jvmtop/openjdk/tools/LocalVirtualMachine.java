/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

/*
 * This file has been modified by jvmtop project authors
 */
package com.jvmtop.openjdk.tools;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.MonitoredVmUtil;
import sun.jvmstat.monitor.VmIdentifier;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


// Sun specific
// Sun private

public class LocalVirtualMachine {
    private String address;

    private final String commandLine;
    private final String displayName;
    private final int vmid;
    private final boolean isAttachSupported;

    private static boolean J9Mode = false;

    static {
        if (System.getProperty("java.vm.name").contains("IBM J9")) {
            J9Mode = true;
            System.setProperty("com.ibm.tools.attach.timeout", "5000");
        }
    }

    public static boolean isJ9Mode() {
        return J9Mode;
    }

    public LocalVirtualMachine(int vmid, String commandLine, boolean canAttach,
                               String connectorAddress) {
        this.vmid = vmid;
        this.commandLine = commandLine;
        this.address = connectorAddress;
        this.isAttachSupported = canAttach;
        this.displayName = getDisplayName(commandLine);
    }

    private static String getDisplayName(String commandLine) {
        // trim the pathname of jar file if it's a jar
        String[] res = commandLine.split(" ", 2);
        if (res[0].endsWith(".jar")) {
            File jarfile = new File(res[0]);
            String displayName = jarfile.getName();
            if (res.length == 2) {
                displayName += " " + res[1];
            }
            return displayName;
        }
        return commandLine;
    }

    public int vmid() {
        return vmid;
    }

    public boolean isManageable() {
        return (address != null);
    }

    public boolean isAttachable() {
        return isAttachSupported;
    }

    public void startManagementAgent() throws IOException {
        if (address != null) {
            // already started
            return;
        }

        if (!isAttachable()) {
            throw new IOException("This virtual machine \"" + vmid
                    + "\" does not support dynamic attach.");
        }

        loadManagementAgent();
        // fails to load or start the management agent
        if (address == null) {
            // should never reach here
            throw new IOException("Fails to find connector address");
        }
    }

    public String connectorAddress() {
        // return null if not available or no JMX agent
        return address;
    }

    public String displayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return commandLine;
    }

    // This method returns the list of all virtual machines currently
    // running on the machine
    public static Map<Integer, LocalVirtualMachine> getAllVirtualMachines() {
        Map<Integer, LocalVirtualMachine> map = new HashMap<>();
        getMonitoredVMs(map, Collections.emptyMap());
        getAttachableVMs(map, Collections.emptyMap());
        return map;
    }

    // This method returns the list of all virtual machines currently
    // running on the machine but not contained in existingVmMap
    public static Map<Integer, LocalVirtualMachine> getNewVirtualMachines(
            Map<Integer, LocalVirtualMachine> existingVmMap) {
        Map<Integer, LocalVirtualMachine> map = new HashMap<>(
                existingVmMap);
        getMonitoredVMs(map, existingVmMap);
        getAttachableVMs(map, existingVmMap);
        return map;
    }

    private static void getMonitoredVMs(Map<Integer, LocalVirtualMachine> map,
                                        Map<Integer, LocalVirtualMachine> existingMap) {
        //Unsupported on J9
        if (J9Mode) {
            return;
        }
        MonitoredHost host;
        Set<Integer> vms;
        try {
            host = MonitoredHost.getMonitoredHost(new HostIdentifier((String) null));
            vms = host.activeVms();
        } catch (URISyntaxException | MonitorException sx) {
            throw new InternalError(sx.getMessage());
        }
        for (Integer vmid : vms) {
            if (existingMap.containsKey(vmid)) {
                continue;
            }
            if (vmid != null) {
                int pid = vmid;
                String name = vmid.toString(); // default to pid if name not available
                boolean attachable = false;
                String address = null;
                try {
                    MonitoredVm mvm = host.getMonitoredVm(new VmIdentifier(name));
                    // use the command line as the display name
                    name = MonitoredVmUtil.commandLine(mvm);
                    attachable = MonitoredVmUtil.isAttachable(mvm);

                    address = importFrom(pid);
                    mvm.detach();
                } catch (Exception x) {
                    x.printStackTrace(System.err);
                }
                map.put(vmid, new LocalVirtualMachine(pid, name, attachable,
                        address));
            }
        }
    }

    private static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress";

    private static void getAttachableVMs(Map<Integer, LocalVirtualMachine> map,
                                         Map<Integer, LocalVirtualMachine> existingVmMap) {
        List<VirtualMachineDescriptor> vms = VirtualMachine.list();
        for (VirtualMachineDescriptor vmd : vms) {
            try {
                Integer vmid = Integer.valueOf(vmd.id());
                if (!map.containsKey(vmid) && !existingVmMap.containsKey(vmid)) {
                    boolean attachable = false;
                    String address = null;
                    try {
                        VirtualMachine vm = VirtualMachine.attach(vmd);
                        attachable = true;
                        Properties agentProps = vm.getAgentProperties();
                        address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
                        vm.detach();
                    } catch (AttachNotSupportedException | NullPointerException x) {
                        // not attachable
                        x.printStackTrace(System.err);
                    } catch (IOException x) {
                        // ignore
                    }
                    map.put(vmid,
                            new LocalVirtualMachine(vmid, vmd.displayName(),
                                    attachable, address));
                }
            } catch (NumberFormatException e) {
                // do not support vmid different than pid
            }
        }
    }

    public static LocalVirtualMachine getLocalVirtualMachine(int vmid) throws IOException, AttachNotSupportedException {
        Map<Integer, LocalVirtualMachine> map = getAllVirtualMachines();
        LocalVirtualMachine lvm = map.get(vmid);
        if (lvm == null) {
            // Check if the VM is attachable but not included in the list
            // if it's running with a different security context.
            // For example, Windows services running
            // local SYSTEM account are attachable if you have Administrator
            // privileges.
            String address;
            String name = String.valueOf(vmid); // default display name to pid

            VirtualMachine vm = VirtualMachine.attach(name);
            Properties agentProps = vm.getAgentProperties();
            address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
            vm.detach();
            lvm = new LocalVirtualMachine(vmid, name, true, address);

        }
        return lvm;
    }

    public static LocalVirtualMachine getDelegateMachine(VirtualMachine vm)
            throws IOException {
        // privileges.
        String address;
        String name = String.valueOf(vm.id()); // default display name to pid

        Properties agentProps = vm.getAgentProperties();
        address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
        vm.detach();
        return new LocalVirtualMachine(Integer.parseInt(vm.id()), name, true,
                address);
    }

    // load the management agent into the target VM
    private void loadManagementAgent() throws IOException {
        VirtualMachine vm;
        String name = String.valueOf(vmid);
        try {
            vm = VirtualMachine.attach(name);
        } catch (AttachNotSupportedException x) {
            throw new IOException(x.getMessage(), x);
        }

        if (isJava9(vm.getSystemProperties())) {
            //running JVM is java 9
            vm.startLocalManagementAgent();

        } else {
            //running JVM is Java 8

            String home = vm.getSystemProperties().getProperty("java.home");

            // Normally in ${java.home}/jre/lib/management-agent.jar but might
            // be in ${java.home}/lib in build environments.

            String agent = home + File.separator + "jre" + File.separator + "lib"
                    + File.separator + "management-agent.jar";
            File f = new File(agent);
            if (!f.exists()) {
                agent = home + File.separator + "lib" + File.separator
                        + "management-agent.jar";
                f = new File(agent);
                if (!f.exists()) {
                    throw new IOException("Management agent not found");
                }
            }

            agent = f.getCanonicalPath();
            try {
                vm.loadAgent(agent, "com.sun.management.jmxremote");
            } catch (AgentLoadException | AgentInitializationException x) {
                throw new IOException(x.getMessage(), x);
            }
        }

        // get the connector address
        if (J9Mode) {
            Properties localProperties = vm.getSystemProperties();
            this.address = ((String) localProperties
                    .get("com.sun.management.jmxremote.localConnectorAddress"));
        } else {
            Properties agentProps = vm.getAgentProperties();
            address = (String) agentProps.get(LOCAL_CONNECTOR_ADDRESS_PROP);
        }

        vm.detach();
    }

    private static boolean isJava9(Properties properties) {
        double javaVersion = Double.parseDouble(properties.getProperty("java.specification.version"));
        return javaVersion >= 1.9;
    }

    private static String importFrom(int pid) {
        if (isJava9(System.getProperties())) {

            try {
                final Class<?> clazz = Class.forName("jdk.internal.agent.ConnectorAddressLink");
                final Method method = clazz.getMethod("importFrom", int.class);
                final Object instance = clazz.getDeclaredConstructor().newInstance();
                return (String) method.invoke(instance, pid);

            } catch (Exception e) {
                throw new IllegalStateException("Could not load needed java 9+ class", e);
            }
        } else {

            try {
                final Class<?> clazz = Class.forName("sun.management.ConnectorAddressLink");
                final Method method = clazz.getMethod("importFrom", Integer.class);
                final Object instance = clazz.getDeclaredConstructor().newInstance();
                return (String) method.invoke(instance, pid);

            } catch (Exception e) {
                throw new IllegalStateException("Could not load needed java 8 class", e);
            }
        }
    }
}
