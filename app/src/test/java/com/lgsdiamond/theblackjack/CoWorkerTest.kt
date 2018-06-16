package com.lgsdiamond.theblackjack

import org.junit.Test

class CoWorkerTest {

    val coWorker = CoWorker()

    @Test
    fun testWorker() {
        coWorker.testDelay()
    }

    @Test
    fun testLotsThread() {
        coWorker.testLotsThread()
    }

    @Test
    fun testLotsLaunch() {
        coWorker.testLotsLaunch()
    }

    @Test
    fun testDeferred() {
        coWorker.testDeferred()
    }

    @Test
    fun testLaunchJob() {
        coWorker.testLaunchJob()
    }

    @Test
    fun testRepeat() {
        coWorker.testRepeat()
    }

    @Test
    fun testCancel() {
        coWorker.testCancel()
    }

    @Test
    fun testCancelCooperative() {
        coWorker.testCancelCooperative()
    }

    @Test
    fun testCancelChecking() {
        coWorker.testCancelChecking()
    }

    @Test
    fun testCancelException() {
        coWorker.testCancelException()
    }

    @Test
    fun testNonCancel() {
        coWorker.testNonCancel()
    }

    @Test
    fun testTimeout() {
        coWorker.testTimeout()
    }

    @Test
    fun testTimeoutOtNull() {
        coWorker.testTimeoutOtNull()
    }

    @Test
    fun testSequential() {
        coWorker.testSequential()
    }

    @Test
    fun testConcurrent() {
        coWorker.testConcurrent()
    }

    @Test
    fun testLazyStart() {
        coWorker.testLazyStart()
    }

    @Test
    fun testAsyncWithoutRunBlocking() {
        coWorker.testAsyncWithoutRunBlocking()
    }

    @Test
    fun testDispatcher() {
        coWorker.testDispatcher()
    }
}