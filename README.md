# JVM Profiling gotchas 
Profilers in are a great tool in a developers toolbox, which is oftern underused or only used when 
there is big performace issue.
In my opions as software engineers we should regularly measure data from our applications, it not only helps us uncover bottlenecks in our software but also helps us gain an indepth understand the execution of code which includes third party libraries and OS level code. Profiling regularly also helps uncover bugs early as it could uncover execution of code which maybe should never be executed in a certian context.
Profiling help us focus our effors on parts of the code which are most critical .

"A good programmer will be wise to look carefully at the critcal coe; but **only after**" that code has been **indetified** " -- D Knuth

"

Without a profiler it is almost impossible to identify critical code in large software.

This post is just some of my notes on how profiling tools (sampling profilers in particular ) work on the JVM and limitation of common tools

## Background

Understanding how profilers work help us to better use profiling tool. We need to cover some background which will be needed for rest of the post.
The JVM itself is large complex piece of software which consists of several major components 
Byte code interpreter, JIT compiler, Garbage-Collector (GC).

javac compiler converts source to class files which are JVM bytecodes, the JVM starts off Java programs by running a byte code interpreter to executing bytecodes the interpreter itself is a simple stack-based machine. While bytecodes are executed by the interpreter, the JVM keeps track of **hot spots** in the code by observing most frequently executed parts of code. To acheive maximal performace the code must execute directly on native cpu, to acheive this, parts of of the code identified as **hot spots** is then compiled to machine code by the JIT (just in time compiler). The code now running on the cpu is could be significantly different from  the source code that was written as JIT compiler does sophisticated optimizations, based on statistics and trace information gathered while executing the bytecodes.
The third main component GC manages the allocation and release of heap based memory, this is a not deterministic process which recycles heap memory that is no longer used by the running application.

### Evaluation of common profilers
To evaluate common profilers we use a sample program with **known** performance bottlenecks 



### Safepoints

The JVM need to carry out some of the core task mentioned above it need to a way to stop all running applications threads, 
so it is **safe** to either recycle memory or compile bycodes to machine code.
JVM threads map to native OS threads and JVM cannot simply ask the OS to stop a thread unless there is some form of cordination.
To allow this the JVM inserts **check points** in executing code which application thread poll to see if the JVM want them to **yield** their execution.
At these points the JVM can signal application thread to yeild themselves so JVM can perform cordinated actions.
The key point here is the JVM cannot force a thread to suspend, it has to wait for the application thread to reach a safepoint before it can be suspended, and once the thread has reached a safepoint it can prevent it from leaving this point until it has finished all is bookeeping work.
Depending on the number of application threads in the system it there is an overhead to brink all application threads to a safepoint.

when running in interpreted mode the interpreter thread will poll the safepoint after executing each bytecode, but for JIT compiled code this is not the case JIT inserts safepoint polling at 
1) backedge of an **uncounted** loops
2) when JNI calls exit
3) method enter/exits
   
### How do most profilers get stack traces
Profilers fall into two categories, sampling profilers and instrumentation profilers.

In a nutshell a sampling profiler periodically request stack traces of each application thread, the stack traces show which method or instruction is currently executing.
This sample is records as the profiler collects more samples we have a **estimate** of the hot code of our application.
For sampling profilers to be effective the following asuumptions **must** be met 

1) Samples are recorded at frequent intervals
2) Need a large collection of samples to get resonably accuratIt's also worth noting that threads blocking in calls to native methods appear in the JVM as RUNNABLE, and hence are reported by VisualVM as Running (and as consuming 100% CPU).e results
3) **All** parts of executing code have **equal probability of being sampled** 

Things to consider when using sampling profilers
1) selecting a sampling interval to avoid values that correspond to periodic events in the application. For example, if a timer interrupt is handled every N milliseconds, we would want to avoid multiples of N as the sampling interval as the profile data can potentially be biased because more often than not we might be sampling in the interrupt handler.
2) Sampling bias, all parts of code should have equal likely hood of being sampled if this is not the case the profile data can be completly misleading.
3) Cost of obtaining samples 
As we shall see many commmon profilers suffer from sampling bias (i.e failing to collect samples randomly) and hot methods can be completely ommited from samples.
Most popular profilers on the JVM use 
[JVM Tool Tnterface ](https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#whatIs)[GetStackTrace](https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#GetStackTrace) to obtain stack trace sample. however there are a couple of major drawbacks when using this interface to sample stack-traces
1) the stack-trace can only be obtained when the application code reaches a **SafePoint** . What this means is the sample can only be of code that can reach a safepoint. In the JVM not all code can reach a safepoint, example in a **counted** loop (ie loop with a bounds know at compile time) a safepoint cannot exist in here, another example is if your application spends a lot of time executing native code via JNI call, this will also not show up in samples, this skews the distribution of the samples which can make the profiler inaccurate. 
2) The other issue is [GetStackTrace](https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html#GetStackTrace) waits for all application threads to reach a safe point before a sample can be taken, this can potentially induce large overheads in the application that is being profiled, to make things worse this is called for each application thread for example if there are 10 application threads running, then collecting a stack sample will casue all application thread to come to a safepoint 10 times. if an application thread that is preempted by the OS but not at a safepoint, we have to wait until this is scheduled back on to the cpu and **has** reached a safepoint. 



#### jVisualVM
potentially misleading JVM RUNNABLE state does not mean thread is actually consuming cpu, it important to be aware of thsi when reading visualVM output.
**threads blocking in calls to native methods appear in the JVM as RUNNABLE, and hence are reported by VisualVM as Running (and as consuming 100% CPU)**


 
