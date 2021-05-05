![https://github.com/aloknnikhil/jvmtop/actions](https://github.com/aloknnikhil/jvmtop/actions/workflows/ci.yaml/badge.svg)

# JVMTop

**Forked from https://github.com/simplewayglobal/jvmtop**

<b>jvmtop</b> is a lightweight console application to monitor all accessible, running jvms on a machine.<br>
In a top-like manner, it displays <a href='https://github.com/aloknnikhil/jvmtop/blob/master/doc/ExampleOutput.md'>JVM internal metrics</a> (e.g. memory information) of running java processes.<br>
<br>
Jvmtop does also include a <a href='https://github.com/aloknnikhil/jvmtop/blob/master/doc/ConsoleProfiler.md'>CPU console profiler</a>.<br>
<br>
It's tested with different releases of Oracle JDK, IBM JDK and OpenJDK on Linux, Solaris, FreeBSD and Windows hosts.<br>
Jvmtop requires a JDK - a JRE will not suffice.<br>
<br>
Please note that it's currently in an alpha state -<br>
if you experience an issue or need further help, please <a href='https://github.com/aloknnikhil/jvmtop/issues'>let us know</a>.<br>
<br>
Jvmtop is open-source. Checkout the <a href='https://github.com/aloknnikhil/jvmtop'>source code</a>. Patches are very welcome!<br>
<br>
Also have a look at the <a href='https://github.com/aloknnikhil/jvmtop/blob/master/doc/Documentation.md'>documentation</a> or at a <a href='https://github.com/aloknnikhil/jvmtop/blob/master/doc/ExampleOutput.md'>captured live-example</a>.<br>

```
 JvmTop 0.8.0 alpha   amd64  8 cpus, Linux 2.6.32-27, load avg 0.12
 https://github.com/aloknnikhil/jvmtop

  PID MAIN-CLASS      HPCUR HPMAX NHCUR NHMAX    CPU     GC    VM USERNAME   #T DL
 3370 rapperSimpleApp  165m  455m  109m  176m  0.12%  0.00% S6U37 web        21
11272 ver.resin.Resin [ERROR: Could not attach to VM]
27338 WatchdogManager   11m   28m   23m  130m  0.00%  0.00% S6U37 web        31
19187 m.jvmtop.JvmTop   20m 3544m   13m  130m  0.93%  0.47% S6U37 web        20
16733 artup.Bootstrap  159m  455m  166m  304m  0.12%  0.00% S6U37 web        46
```

<hr />

<h3>Installation</h3>
Click on the <a href="https://github.com/aloknnikhil/jvmtop/releases"> releases tab</a>, download the
most recent tar.gz archive. Extract it, ensure that the `JAVA_HOME` environment variable points to a valid JDK and run `./jvmtop.sh`.<br><br>
Further information can be found in the [INSTALL file](https://github.com/aloknnikhil/jvmtop/blob/master/INSTALL)



<h3>05/04/2021 jvmtop 1.0.3 released</h3>
<b>Changes:</b>
<ul>
<li>addressed warnings<br></li>
<li>updated for Java 11<br></li>
</ul>

<a href='https://github.com/aloknnikhil/jvmtop/blob/master/doc/Changelog.md'>Full changelog</a>

<hr />

In <a href='https://github.com/aloknnikhil/jvmtop/blob/master/doc/ExampleOutput.md'>VM detail mode</a> it shows you the top CPU-consuming threads, beside detailed metrics:<br>
<br>
<br>

```
 JvmTop 0.8.0 alpha   amd64,  4 cpus, Linux 2.6.18-34
 https://github.com/aloknnikhil/jvmtop

 PID 3539: org.apache.catalina.startup.Bootstrap
 ARGS: start
 VMARGS: -Djava.util.logging.config.file=/home/webserver/apache-tomcat-5.5[...]
 VM: Sun Microsystems Inc. Java HotSpot(TM) 64-Bit Server VM 1.6.0_25
 UP: 869:33m #THR: 106  #THRPEAK: 143  #THRCREATED: 128020 USER: webserver
 CPU:  4.55% GC:  3.25% HEAP: 137m / 227m NONHEAP:  75m / 304m
  TID   NAME                                    STATE    CPU  TOTALCPU BLOCKEDBY
     25 http-8080-Processor13                RUNNABLE  4.55%     1.60%
 128022 RMI TCP Connection(18)-10.101.       RUNNABLE  1.82%     0.02%
  36578 http-8080-Processor164               RUNNABLE  0.91%     2.35%
  36453 http-8080-Processor94                RUNNABLE  0.91%     1.52%
     27 http-8080-Processor15                RUNNABLE  0.91%     1.81%
     14 http-8080-Processor2                 RUNNABLE  0.91%     3.17%
 128026 JMX server connection timeout   TIMED_WAITING  0.00%     0.00%
```

<a href='https://github.com/aloknnikhil/jvmtop/issues'>Pull requests / bug reports</a> are always welcome.<br>
<br>
