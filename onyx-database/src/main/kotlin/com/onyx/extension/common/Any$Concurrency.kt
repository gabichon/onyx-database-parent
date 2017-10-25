package com.onyx.extension.common

import java.util.concurrent.*

typealias Block = Any

val defaultPool: ExecutorService = Executors.newWorkStealingPool()!!

/**
 * Run a job using a single thread executor.  This will run a daemon thread and pause between iterations to save CPU
 * cycles.
 *
 * @param block Block expression to execute
 * @since 2.0.0
 */
fun runJob(interval:Long, unit:TimeUnit, block: () -> Unit): Job {
    val executor = Executors.newSingleThreadExecutor()

    val job = Job(executor = executor)
    job.future = executor.submit {
        while (job.active) {
            block.invoke()
            delay(interval, unit)
        }
    }

    return job
}

/**
 * Run a block in background on the default pool.  The default pool should be implemented as a ForkJoinPool
 *
 * @param block Block expression to execute
 * @since 2.0.0
 */
fun <T> async(block: () -> T): Future<T> = defaultPool.submit<T> { block.invoke() }

/**
 * Sleep thread a fixed amount of time.
 *
 * @param amount of time to sleep
 * @param unit TimeUnit value
 * @since 2.0.0
 */
fun delay(amount:Long, unit: TimeUnit) = Thread.sleep(unit.toMillis(amount))

/**
 * Job.  This class is a handle of a single thread executor running a daemon thread
 * @since 2.0.0
 */
class Job(private val executor:ExecutorService) {

    var active = true
    var future:Future<*>? = null

    /**
     * Cancel the job and shut down the executor
     * @since 2.0.0
     */
    fun cancel() {
        active = false
        future?.cancel(true)
        executor.shutdownNow()
    }

    /**
     * Join the job thread and block until completed
     * @since 2.0.0
     */
    fun join() {
        future?.get()
    }
}

/**
 * Deferred Value is a substitute for Completable future.  Since we do not want to rely on Java 8 because of Android
 * this was added.  The only difference is that this uses a count down latch but functionality should be the same
 *
 * @since 2.0.0
 */
class DeferredValue<T> {

    val countDown = CountDownLatch(1)
    var value:T? = null

    fun complete(value:T?) {
        this.value = value
        countDown.countDown()
    }

    fun get(timeout:Long, unit: TimeUnit):T? {
        countDown.await(timeout, unit)
        return value
    }
}