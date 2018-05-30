package com.lgsdiamond.theblackjack.blackjackelement

/**
 * Created by LgsDi on 2018-03-10.
 */
enum class CardDealRule { DEAL_ONCE, DEAL_LATE }   // card dealing order

enum class DoubleDownRule { ANY_TWO, NINE_TEN_ELEVEN, TEN_ELEVEN }

data class GameRule(var surrenderAllowed: Boolean,
                    var blackjackRate: Float, var maxSplitCount: Int,
                    var cardDealRule: CardDealRule, var doubleDownRule: DoubleDownRule,
                    var doubleAllowedAfterSplit: Boolean,
                    var moreThanOnceAceSplitAllowed: Boolean,
                    var justOneCardAfterAceSplit: Boolean,
                    var peekAllowed: Boolean,
                    var hitOnDealerSoft17: Boolean,
                    var allowAceResplit: Boolean,
                    var allowSplitDifferentTenValues: Boolean) {

    companion object {
        val sVegasStripRule = GameRule(false,
                1.5f, 3,
                CardDealRule.DEAL_ONCE, DoubleDownRule.ANY_TWO,
                true,
                false,
                false,
                true,
                false,
                false,
                true)

        val sAtlanticCityRule = GameRule(false,
                1.5f, 3,
                CardDealRule.DEAL_ONCE, DoubleDownRule.ANY_TWO,
                true,
                false,
                true,
                true,
                false,
                false,
                true)

        val sEuropeanRule = GameRule(false,
                1.5f, 3,
                CardDealRule.DEAL_LATE, DoubleDownRule.ANY_TWO,
                true,
                false,
                true,
                false,
                true,
                false,
                true)

        val sTestRule = GameRule(true,
                1.5f, 99,
                CardDealRule.DEAL_ONCE, DoubleDownRule.ANY_TWO,
                true,
                true,
                true,
                true,
                true,
                false,
                true)
    }

    fun readStatus() {

    }


    fun writeStatus() {

    }
}

data class TableRule(var numDecks: Int, var numBoxes: Int, var minBet: Float, var maxBet: Float,
                     var initBalance: Float, var blackjackPayout: Float, var fixedRandom: Boolean,
                     var useSound: Boolean, var useAnimation: Boolean) {
    companion object {
        val sDefaultTableRule = TableRule(8, 10, 10.0f, 1_000.0f,
                10_000.0f, 1.5f, true,
                true, true)
    }

    fun readStatus() {

    }


    fun writeStatus() {

    }
}