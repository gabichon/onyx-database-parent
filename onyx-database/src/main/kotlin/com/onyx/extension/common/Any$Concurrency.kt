package com.onyx.extension.common

import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext

typealias Block = Any

val threadsForReuse = ArrayList<CoroutineContext>()

/**
 * Run a job using the CommonPool
 *
 * @param block Block expression to execute
 */
fun runJob(name: String, block: suspend CoroutineScope.() -> Unit): Job {
    val context = if(!threadsForReuse.isEmpty()) {
        threadsForReuse.removeAt(0)
    } else {
        newSingleThreadContext(name)
    }
    val job = launch(context, block = block)
    job.invokeOnCompletion {
        synchronized(threadsForReuse) {
            job.start() // This is needed upon canceling a job.  Apparently it will stop the executor.  The start will resume
            threadsForReuse.add(context)
       }
    }
    return job
}

/**
 * Run a block in background co-routine
 *
 * @param block Block expression to execute
 */
fun <T> async(block: suspend CoroutineScope.() -> T): Deferred<T> = kotlinx.coroutines.experimental.async(context = CommonPool, block = block)
