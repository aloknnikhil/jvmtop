/*
 * Copyright (C) 2021 by Alok Nandan Nikhil. All rights reserved.
 */

/*
 * jvmtop - java monitoring for the command-line
 * <p>
 * Copyright (C) 2013 by Patric Rufflar. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * <p>
 * <p>
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.jvmtop.view;

import com.jvmtop.JvmTop;
import com.jvmtop.monitor.VMInfo;
import com.jvmtop.monitor.VMInfoState;
import com.jvmtop.openjdk.tools.LocalVirtualMachine;
import com.jvmtop.profiler.CPUSampler;
import com.jvmtop.profiler.CachegrindVisualizer;
import com.jvmtop.profiler.CalltreeNode;
import com.jvmtop.profiler.FlameVisualizer;
import com.jvmtop.profiler.JsonVisualizer;
import com.jvmtop.profiler.TreeVisualizer;
import com.jvmtop.profiler.Visualizer;
import com.sun.tools.attach.AttachNotSupportedException;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

/**
 * CPU sampling-based profiler view which shows methods with top CPU usage.
 *
 * @author paru
 *
 */
public class VMProfileView extends AbstractConsoleView {

    private final CPUSampler cpuSampler_;
    private final VMInfo vmInfo_;
    private final JvmTop.Config config_;

    public VMProfileView(int vmid, JvmTop.Config config) throws IOException, AttachNotSupportedException {
        super(config.width);
        LocalVirtualMachine localVirtualMachine = LocalVirtualMachine
                .getLocalVirtualMachine(vmid);
        vmInfo_ = VMInfo.processNewVM(localVirtualMachine, vmid);
        config_ = config;
        cpuSampler_ = new CPUSampler(vmInfo_, config_);
    }

    private static void renderToFile(String fileName, Double minTotal, Integer threadsLimit,
                                     CPUSampler cpuSampler_, Visualizer visualizer, PrintStream defaultOut)
            throws FileNotFoundException {
        if (fileName == null && defaultOut == null) return;
        PrintStream out = defaultOut;
        try {
            if (fileName != null)
                out = new PrintStream(new FileOutputStream(fileName));
            visualizer.start(out);
            for (CalltreeNode node : cpuSampler_.getTop(minTotal, threadsLimit)) {
                visualizer.print(node, out);
            }
            visualizer.end(out);
        } finally {
            if (fileName != null && out != null) {
                out.close();
                System.out.println("Printed dump to file: " + fileName);
            }
        }
    }

    @Override
    public void sleep(long millis) throws InterruptedException {
        long cur = System.currentTimeMillis();
        cpuSampler_.update();
        while (cur + millis > System.currentTimeMillis()) {
            cpuSampler_.update();
            super.sleep(100);
        }

    }

    @Override
    public void printView() throws FileNotFoundException {
        if (vmInfo_.getState() == VMInfoState.ATTACHED_UPDATE_ERROR) {
            System.out.println("ERROR: Could not fetch telemetries - Process terminated?");
            exit();
            return;
        }
        if (vmInfo_.getState() != VMInfoState.ATTACHED) {
            System.out.println("ERROR: Could not attach to process.");
            exit();
            return;
        }

        int w = width - 40;
        System.out.printf(" Profiling PID %d: %40s %n%n", vmInfo_.getId(),
                leftStr(vmInfo_.getDisplayName(), w));

        long processTotalTime = cpuSampler_.getTotal();
        if (processTotalTime < 1) return;
        renderToFile(config_.fileVisualize, config_.minTotal, config_.threadlimit, cpuSampler_, new TreeVisualizer(config_, processTotalTime), System.out);
        renderToFile(config_.jsonVisualize, config_.minTotal, config_.threadlimit, cpuSampler_, new JsonVisualizer(config_, processTotalTime), null);
    }

    @Override
    public void last() throws FileNotFoundException {
        renderToFile(config_.cachegrindVisualize, config_.minTotal, config_.threadlimit, cpuSampler_, new CachegrindVisualizer(), null);
        renderToFile(config_.flameVisualize, config_.minTotal, config_.threadlimit, cpuSampler_, new FlameVisualizer(), null);
    }
}
