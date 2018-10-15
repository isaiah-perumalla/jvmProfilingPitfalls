# Pit falls of profiling JVM apps 
Profilers in are a great tool in a developers toolbox, which is oftern underused or only used when 
there is big performace issue.
In my opions as software devs we should regularly profile code as it not only helps us uncover bottlenecks in our software but also
helps us gain an indepth understand the execution of code which includes third party libraries and OS level code. 
Profiling help us focus our effors on parts of the code which are most critical .

"A good programmer will be wise to look carefully at the critcal coe; but **only after**" that code has been **indetified** " -- D Knuth

"

Without a profiler it is almost impossible to indentify critical code in large software.

This post is just some of my notes on how profiing tools (sampling profilers in particular ) work on the JVM and limitation of common tools

## Background

Indentifing inefficiencies in software requires a much software is actaully executed, for applications running on JVM , we need to cover some 
background which will be needed for rest of the post.
The JVM itself is large complex piece of software which consists of several major components 
Byte code interpreter, JIT compiler, Garbage-Collector (GC).

JVM starts off Java programs by executing bytecodes usig the bytecode interpreter, which is a stack-based machine, during this time
JVM keeps track **hot spots** in the code by observing most frequently executed part of code, this part of the code is then compiled to machine code by the JIT (just in time compier)
The third main component GC manages the allocation and release of heap based memory, this is a not deterministic process which recycles heap memory that is no longer used by the running application.


### Safepoints

The JVM need to carry out some of the core task mentioned above it need to a way to stop all running applications threads, 
so it is **safe** to either recycle memory or compile bycodes to machine code.
JVM threads map to native OS threads and JVM cannot simply ask the OS to stop a thread unless there is some form of cordination.
To allow this the JVM inserts **check points** in executing code which application thread poll to see if the JVM want them to **yield** their execution.
At these points the JVM can signal application thread to yeild themselves so JVM can perform cordinationed actions.
The key point here is the JVM cannot force a thread to suspend, it has to wait for the application thread to reach a safepoint before it can be suspended, and once the thread
has reached a safepoint it can prevent it from leaving this point until it has finished all is bookeeping work.
Depending on the number of application threads in the system it there is an overhead to brink all application threads to a safepoint.

when running in interpreeted mode the running thread will poll the safepoint after executing each bytecode, but of JIT compiled code this is not the case 
JIT inserts safepoint polling only at method exits.
   
### How do most profilers get stack traces
Profilers fall into two categories, sampling profilers and instrumentation profilers.

In a nutshell a sampling profiler periodically request stack traces of each application thread, the stack traces show which method or instruction is currently executing.
This sample is recordes as the profiler collects more samples we have a **estimate** of the hot code of our application.
For sampling profilers to be effective the following asuumptions **must** be met 

1) Samples are recorded at frequent intervals
2) Need a large collection of samples to get resonably accurate results
3) **All** parts of exectuing code have **equal probability of being sampled** 




 