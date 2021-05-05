package com.jvmtop.profiler;

import java.io.PrintStream;

public class CachegrindVisualizer implements Visualizer {
    @Override
    public void start(PrintStream out) {
        out.println("events: Instructions");
        out.println();
    }

    @Override
    public void print(CalltreeNode node, PrintStream out) {
        for (CalltreeNode child : node.getChildren()) { // really only one child there
            printInternal(child, out);
        }
    }

    @Override
    public void end(PrintStream out) {

    }

    private static void printInternal(CalltreeNode node, PrintStream out) {
        StackTraceElement element = node.getStackTraceElement();
        out.println("fl=" + element.getFileName());
        out.println("fn=" + node.getName());
        out.println(element.getLineNumber() + " " + node.getSelf());

        for (CalltreeNode child : node.getChildren()) {
            out.println("cfl=" + child.getStackTraceElement().getFileName());
            out.println("cfn=" + child.getName());
            out.println("calls=" + child.getCalls() + " " + child.getStackTraceElement().getLineNumber());
            out.println(element.getLineNumber() + " " + child.getTotalTime());
        }

        out.println();

        for (CalltreeNode child : node.getChildren()) {
            printInternal(child, out);
        }

    }
}
