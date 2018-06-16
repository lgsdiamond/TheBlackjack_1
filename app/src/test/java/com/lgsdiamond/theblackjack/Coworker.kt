package com.lgsdiamond.theblackjack

import kotlinx.coroutines.experimental.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.system.measureTimeMillis

class CoWorker {
    val testValue: Int = 0

    fun testDelay() {
        println("Start")

        // Start a coroutine
        launch {
            delay(1000)
            println("Hello")
        }

        Thread.sleep(2000) // wait for 2 seconds
        println("Stop")
    }

    fun testLotsThread() {
        val c = AtomicInteger()

        for (i in 1..1_000_000)
            thread(start = true) {
                c.addAndGet(i)
            }

        println(c.get())
    }

    fun testLotsLaunch() {
        val c = AtomicInteger()

        for (i in 1..1_000_000)
            launch {
                c.addAndGet(i)
            }

        println(c.get())
    }

    var staringTime = 0L
    fun setStart() {
        staringTime = System.currentTimeMillis()
        println("Starting: $staringTime")
    }

    fun showTime(note: String) {
        println("$note: ${System.currentTimeMillis() - staringTime}")
    }

    fun testDeferred() {

        setStart()

        val deferred = (1..1_000_000).map { n ->
            async {
                workload(n)
            }
        }

        showTime("Before runBlocking")
        runBlocking {

            showTime("Before runBlocking")

            val sum = deferred.sumBy { it.await() }
            println("Sum: $sum")

            showTime("Ending")
        }
    }

    suspend fun workload(n: Int): Int {
        if (n % 100_000 == 0) showTime("$n count")
        delay(1000)
        return n
    }

    fun testLaunchJob() = runBlocking {
        val job = launch {
            doWorld()
        }
        println("Hello,")
        job.join() // wait until child coroutine completes
    }

    private suspend fun doWorld() {
        delay(1000L)
        println("World!")
    }

    fun testRepeat() = runBlocking {
        repeat(10) { i ->
            println("I'm sleeping $i ...")
            delay(500L)
        }

        launch {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        delay(1300L) // just quit after delay
    }

    fun testCancel() = runBlocking<Unit> {
        val job = launch {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit.")
    }

    fun testCancelCooperative() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val job = launch {
            var nextPrintTime = startTime
            var i = 0
            while (i < 10) { // computation loop, just wastes CPU. CANNOT Cancel!!
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }

    fun testCancelChecking() = runBlocking<Unit> {
        val startTime = System.currentTimeMillis()
        val job = launch {
            var nextPrintTime = startTime
            var i = 0
            while (isActive) { // cancellable computation loop
                // print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }

    fun testCancelException() = runBlocking<Unit> {
        val job = launch {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                println("I'm running finally")
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }

    fun testNonCancel() = runBlocking<Unit> {
        val job = launch {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                println("I'm running finally")
                withContext(NonCancellable) {
                    delay(1000L)
                    println("And I've just delayed for 1 sec because I'm non-cancellable")
                }
            }
        }
        delay(1300L) // delay a bit
        println("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        println("main: Now I can quit.")
    }

    fun testTimeout() = runBlocking<Unit> {
        try {
            withTimeout(1300L) {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            }
        } catch (e: TimeoutCancellationException) {
            println("Timeout Exception")
        } finally {
            println("Finally Job")
        }
    }

    fun testTimeoutOtNull() = runBlocking<Unit> {
        withTimeoutOrNull(1300L) {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
    }

    fun testSequential() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            println("The answer is ${one + two}")
        }
        println("Completed in $time ms")
    }

    fun testConcurrent() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }

    fun testLazyStart() = runBlocking<Unit> {
        val time = measureTimeMillis {
            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }
        println("Completed in $time ms")
    }

    // note, that we don't have `runBlocking` to the right of `main` in this example
    fun testAsyncWithoutRunBlocking() {
        val time = measureTimeMillis {
            // we can initiate async actions outside of a coroutine
            val one = somethingUsefulOneAsync()
            val two = somethingUsefulTwoAsync()
            // but waiting for a result must involve either suspending or blocking.
            // here we use `runBlocking { ... }` to block the main thread while waiting for the result
            runBlocking {
                println("The answer is ${one.await() + two.await()}")
            }
        }
        println("Completed in $time ms")
    }

    suspend fun doSomethingUsefulOne(): Int {
        delay(1000L) // pretend we are doing something useful here
        return 13
    }

    suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L) // pretend we are doing something useful here, too
        return 29
    }

    // The result type of somethingUsefulOneAsync is Deferred<Int>
    fun somethingUsefulOneAsync() = async {
        doSomethingUsefulOne()
    }

    // The result type of somethingUsefulTwoAsync is Deferred<Int>
    fun somethingUsefulTwoAsync() = async {
        doSomethingUsefulTwo()
    }

    // Dispatchers and threads

    fun testDispatcher() = runBlocking<Unit> {
        val jobs = arrayListOf<Job>()
        jobs += launch(Unconfined) {
            // not confined -- will work with main thread
            println("      'Unconfined': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(coroutineContext) {
            // context of the parent, runBlocking coroutine
            println("'coroutineContext': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(CommonPool) {
            // will get dispatched to ForkJoinPool.commonPool (or equivalent)
            println("      'CommonPool': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(newSingleThreadContext("MyOwnThread")) {
            // will get its own new thread
            println("          'newSTC': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs.forEach { it.join() }
    }
}