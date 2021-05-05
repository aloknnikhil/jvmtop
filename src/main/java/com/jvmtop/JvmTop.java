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
package com.jvmtop;

import com.jvmtop.view.ConsoleView;
import com.jvmtop.view.VMDetailView;
import com.jvmtop.view.VMOverviewView;
import com.jvmtop.view.VMProfileView;
import picocli.CommandLine;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JvmTop entry point class.
 *
 * - parses program arguments
 * - selects console view
 * - prints header
 * - main "iteration loop"
 *
 * @author paru / aloknnikhil
 *
 */
public class JvmTop implements Callable<Void> {
    private final static String CLEAR_TERMINAL_ANSI_CMD = new String(new byte[]{
            (byte) 0x1b, (byte) 0x5b, (byte) 0x32, (byte) 0x4a, (byte) 0x1b,
            (byte) 0x5b, (byte) 0x48});
    private final java.lang.management.OperatingSystemMXBean localOSBean_;
    private final Config config = new Config();

    private static Logger logger;
    private Boolean supportsSystemAverage_;

    public JvmTop() {
        localOSBean_ = ManagementFactory.getOperatingSystemMXBean();
    }

    public static void main(String... args) {
        Locale.setDefault(Locale.US);
        logger = Logger.getLogger("jvmtop");

        JvmTop jvmTop = new JvmTop();
        CommandLine commandLine = new CommandLine(jvmTop.config);
        CommandLine.ParseResult parseResult = commandLine.parseArgs(args);

        if (commandLine.isUsageHelpRequested()) {
            commandLine.usage(commandLine.getOut());
            System.exit(commandLine.getCommandSpec().exitCodeOnUsageHelp());
        } else if (commandLine.isVersionHelpRequested()) {
            commandLine.printVersionHelp(commandLine.getOut());
            System.exit(commandLine.getCommandSpec().exitCodeOnVersionHelp());
        }

        try {
            commandLine.setExecutionResult(jvmTop.call());
        } catch (Exception e) {
            logger.severe(e.getMessage());
            System.exit(commandLine.getCommandSpec().exitCodeOnExecutionException());
        }
        System.exit(commandLine.getCommandSpec().exitCodeOnSuccess());
    }

    private static void fineLogging() {
        //get the top Logger:
        Logger topLogger = java.util.logging.Logger.getLogger("");

        // Handler for console (reuse it if it already exists)
        Handler consoleHandler = null;
        //see if there is already a console handler
        for (Handler handler : topLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                //found the console handler
                consoleHandler = handler;
                break;
            }
        }

        if (consoleHandler == null) {
            //there was no console handler found, create a new one
            consoleHandler = new ConsoleHandler();
            topLogger.addHandler(consoleHandler);
        }
        //set the console handler to fine:
        consoleHandler.setLevel(java.util.logging.Level.FINEST);
    }

    private static void outputSystemProps() {
        for (Object key : System.getProperties().keySet()) {
            System.out.println(key + "=" + System.getProperty(key + ""));
        }
    }

    private static void registerShutdown(final ConsoleView view) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                System.out.print("Finish execution ... ");
                view.last();
                System.out.println("done!");
            } catch (Exception e) {
                System.err.println("Failed to start last in shutdown");
                e.printStackTrace();
            }
        }));
    }

    protected void start(final ConsoleView view) throws Exception {
        try {
            System.setOut(new PrintStream(new BufferedOutputStream(
                    new FileOutputStream(FileDescriptor.out)), false));
            int iterations = 0;
            registerShutdown(view);
            while (!view.shouldExit()) {
                if (this.config.iterations > 1 || this.config.iterations == -1) {
                    clearTerminal();
                }
                printTopBar();
                view.printView();
                System.out.flush();
                iterations++;
                if (iterations >= this.config.iterations && this.config.iterations > 0) {
                    break;
                }
                view.sleep((int) (this.config.delay * 1000));
            }
//      view.last();
        } catch (NoClassDefFoundError e) {
            e.printStackTrace(System.err);

            System.err.println();
            System.err.println("ERROR: Some JDK classes cannot be found.");
            System.err.println("Please check if the JAVA_HOME environment variable has been set to a JDK path.");
            System.err.println();
        }
    }

    /**
     *
     */
    private void clearTerminal() {
        if (System.getProperty("os.name").contains("Windows")) {
            //hack
            System.out
                    .printf("%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n");
        } else if (System.getProperty("jvmtop.altClear") != null) {
            System.out.print('\f');
        } else {
            System.out.print(CLEAR_TERMINAL_ANSI_CMD);
        }
    }

    /**
     * @throws SecurityException
     *
     */
    private void printTopBar() {
        StringBuilder version = new StringBuilder();
        for (String versionPart : Config.class.getAnnotation(CommandLine.Command.class).version()) {
            version.append(versionPart);
        }
        System.out.printf(" JvmTop %s - %8tT, %6s, %2d cpus, %15.15s", version,
                new Date(), localOSBean_.getArch(),
                localOSBean_.getAvailableProcessors(), localOSBean_.getName() + " "
                        + localOSBean_.getVersion());

        if (supportSystemLoadAverage() && localOSBean_.getSystemLoadAverage() != -1) {
            System.out.printf(", load avg %3.2f%n",
                    localOSBean_.getSystemLoadAverage());
        } else {
            System.out.println();
        }
        System.out.println(" https://github.com/aloknnikhil/jvmtop");
        System.out.println();
    }

    private boolean supportSystemLoadAverage() {
        if (supportsSystemAverage_ == null) {
            try {
                supportsSystemAverage_ = true;
            } catch (Throwable e) {
                supportsSystemAverage_ = false;
            }
        }
        return supportsSystemAverage_;
    }

    @Override
    public Void call() throws Exception {
        if (config.delay < 0.1d) {
            throw new IllegalArgumentException("Delay cannot be set below 0.1");
        }

        if (config.verbose) {
            fineLogging();
            logger.setLevel(Level.ALL);
            logger.fine("Verbosity mode.");
        }

        if (config.pidParameter != null) {
            // support for parameter w/o name
            config.pid = config.pidParameter;
        }

        if (config.sysInfoOption) {
            outputSystemProps();
        } else {
            if (config.pid == null) {
                start(new VMOverviewView(config.width));
            } else {
                if (config.profileMode) {
                    start(new VMProfileView(config.pid, config));
                } else {
                    VMDetailView vmDetailView = new VMDetailView(config.pid, config.width);
                    vmDetailView.setDisplayedThreadLimit(config.threadLimitEnabled);
                    if (config.threadlimit != null) {
                        vmDetailView.setNumberOfDisplayedThreads(config.threadlimit);
                    }
                    if (config.threadNameWidth != null) {
                        vmDetailView.setThreadNameDisplayWidth(config.threadNameWidth);
                    }
                    start(vmDetailView);
                }
            }
        }
        return null;
    }

    @CommandLine.Command(mixinStandardHelpOptions = true, versionProvider = JvmTop.PropertiesVersionProvider.class)
    public static class Config {
        @CommandLine.Option(names = {"-i", "--sysinfo"}, description = "Outputs diagnostic information")
        public boolean sysInfoOption = false;
        @CommandLine.Option(names = {"-v", "--verbose"}, description = "Outputs verbose logs")
        public boolean verbose = false;

        @CommandLine.Parameters(index = "0", arity = "0..1", description = "PID to connect to, override parameter")
        public Integer pidParameter = null;
        @CommandLine.Option(names = {"-p", "--pid"}, description = "PID to connect to")
        public Integer pid = null;

        @CommandLine.Option(names = {"-w", "--width"}, description = "Width in columns for the console display")
        public Integer width = 280;

        @CommandLine.Option(names = {"-d", "--delay"}, description = "Delay between each output iteration")
        public double delay = 1.0;

        @CommandLine.Option(names = "--profile", description = "Start CPU profiling at the specified jvm")
        public boolean profileMode = false;

        @CommandLine.Option(names = {"-n", "--iteration"}, description = "jvmtop will exit after n output iterations")
        public Integer iterations = -1;

        @CommandLine.Option(names = "--threadlimit", description = "sets the number of displayed threads in detail mode")
        public Integer threadlimit = Integer.MAX_VALUE;

        @CommandLine.Option(names = "--disable-threadlimit", description = "displays all threads in detail mode")
        public boolean threadLimitEnabled = true;

        @CommandLine.Option(names = "--threadnamewidth", description = "sets displayed thread name length in detail mode (defaults to 30)")
        public Integer threadNameWidth = null;

        @CommandLine.Option(names = "--profileMinTotal", description = "Profiler minimum thread cost to be in output")
        public Double minTotal = 5.0;
        @CommandLine.Option(names = "--profileMinCost", description = "Profiler minimum function cost to be in output")
        public Double minCost = 5.0;
        @CommandLine.Option(names = "--profileMaxDepth", description = "Profiler maximum function depth in output")
        public Integer maxDepth = 15;
        @CommandLine.Option(names = "--profileCanSkip", description = "Profiler ability to skip intermediate functions with same cpu usage as their parent")
        public boolean canSkip = false;
        @CommandLine.Option(names = "--profilePrintTotal", description = "Profiler printing percent of total thread cpu")
        public boolean printTotal = false;
        @CommandLine.Option(names = "--profileRealTime", description = "Profiler uses real time instead of cpu time (usable for sleeps profiling)")
        public boolean profileRealTime = false;
        @CommandLine.Option(names = "--profileFileVisualize", description = "Profiler file to output result")
        public String fileVisualize = null;
        @CommandLine.Option(names = "--profileJsonVisualize", description = "Profiler file to output result (JSON format)")
        public String jsonVisualize = null;
        @CommandLine.Option(names = "--profileCachegrindVisualize", description = "Profiler file to output result (Cachegrind format)")
        public String cachegrindVisualize = null;
        @CommandLine.Option(names = "--profileFlameVisualize", description = "Profiler file to output result (Flame graph format)")
        public String flameVisualize = null;
        @CommandLine.Option(names = "--profileThreadIds", description = "Profiler thread ids to profile (id is #123 after thread name)", split = ",", type = Long.class)
        public List<Long> profileThreadIds = new ArrayList<>();
        @CommandLine.Option(names = "--profileThreadNames", description = "Profiler thread names to profile", split = ",")
        public List<String> profileThreadNames = new ArrayList<>();

        public Config() {
        }
    }

    static class PropertiesVersionProvider implements CommandLine.IVersionProvider {
        private static final String PROPERTIES_FILE = "project.properties";

        @Override
        public String[] getVersion() throws IOException {
            final Properties properties = new Properties();
            properties.load(this.getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE));
            return new String[] {
                    properties.getProperty("appName") + " version \"" + properties.getProperty("version") + "\"",
                    "Built: " + properties.getProperty("buildTimestamp"),
            };
        }
    }
}
