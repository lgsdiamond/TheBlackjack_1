package com.lgsdiamond.theblackjack

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CalculatorTest {
    private val calculator = Calculator()

    @Before
    fun setUp() {
    }

    @After
    fun tearDown() {
    }

    @Test
    fun add() {
        assertEquals(3, calculator.add(1, 2))
    }

    @Test
    fun sub() {
        assertEquals(-1, calculator.sub(1, 2))
    }
}
