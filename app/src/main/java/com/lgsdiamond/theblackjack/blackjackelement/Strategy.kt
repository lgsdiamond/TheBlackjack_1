package com.lgsdiamond.theblackjack.blackjackelement

import com.lgsdiamond.theblackjack.toToastShort


/**
 * HIT, STAND, SPLIT
 * SURRENDER_HIT/SURRENDER_STAND/SURRENDER_SPLIT: Surrender if allowed, otherwise Hit/Stand/Split
 * DOUBLE_HIT/DOUBLE_STAND: Double if allowed, otherwise Hit/Stand
 * SPLIT_HIT: Split if double after split is allowed, otherwise hit
 */

enum class PlayerAction(val code: String) { UNKNOWN("?"), SURRENDER("R"),
    HIT("H"), STAND("S"), SPLIT("P"), DOUBLEDOWN("D"),
    SURRENDER_HIT("Rh"), SURRENDER_STAND("Rs"), SURRENDER_SPLIT("Rp"),
    DOUBLE_HIT("Dh"), DOUBLE_STAND("Ds"),
    SPLIT_HIT("Ph");

    companion object {
        fun findByCode(code: String): PlayerAction {
            for (action in values().iterator()) {
                if (action.code.contentEquals(code)) return (action)
            }
            return (PlayerAction.UNKNOWN)
        }
    }
}

/**
 * Created by lgsdi on 2015-12-26.
 */
internal typealias PlayerActionList = ArrayList<PlayerAction>

internal typealias StringArray = Array<String>

class Strategy(val title: String, val description: String,
               hard: Array<StringArray>, soft: Array<StringArray>, pair: Array<StringArray>) {

    private val mHardActions = makeActionsFromCodes(COUNT_HARD_ACTIONS, hard)
    private val mSoftActions = makeActionsFromCodes(COUNT_SOFT_ACTIONS, soft)
    private val mPairActions = makeActionsFromCodes(COUNT_PAIR_ACTIONS, pair)

    fun consultBestAction(pHand: PlayerHand, dealerUpScore: Int): PlayerAction {
        val dealerUpIndex = if (dealerUpScore == 1) 9 else dealerUpScore - 2

        fun bestSplitAction(): PlayerAction {
            val score = pHand[0].score
            val playerIndex = if (score == 1) (COUNT_SOFT_ACTIONS - 1) else (score - 2)    // (2,2)->0, ..., (A,A)->9
            return mPairActions[playerIndex][dealerUpIndex]
        }

        fun bestSoftAction(): PlayerAction {
            val playerIndex = pHand.handScore - 13  // A2->0, A3->1, ..., A9->7. total size = 8
            return mSoftActions[playerIndex][dealerUpIndex]
        }

        fun bestHardAction(): PlayerAction {
            val score = pHand.handScore
            val playerIndex = when {
                (score <= 8) -> 0               // 0-8->0
                (score <= 17) -> (score - 8)    // 9->1, 10->2, ..., 17->9
                else -> COUNT_HARD_ACTIONS - 1    // 18+->10, total size = 11
            }
            return mHardActions[playerIndex][dealerUpIndex]
        }

        fun screenCombinedActions(action: PlayerAction): PlayerAction = when (action) {
            PlayerAction.SURRENDER_HIT -> {
                if (pHand.canSurrender) PlayerAction.SURRENDER else PlayerAction.HIT
            }
            PlayerAction.SURRENDER_SPLIT -> {
                if (pHand.canSurrender) PlayerAction.SURRENDER else PlayerAction.SPLIT
            }
            PlayerAction.SURRENDER_STAND -> {
                if (pHand.canSurrender) PlayerAction.SURRENDER else PlayerAction.STAND
            }

            PlayerAction.DOUBLE_HIT -> {
                if (pHand.canDoubleDown) PlayerAction.DOUBLEDOWN else PlayerAction.HIT
            }
            PlayerAction.DOUBLE_STAND -> {
                if (pHand.canDoubleDown) PlayerAction.DOUBLEDOWN else PlayerAction.STAND
            }

            PlayerAction.SPLIT_HIT -> {
                if (Table.gameRule.doubleAllowedAfterSplit) PlayerAction.SPLIT else PlayerAction.HIT
            }

            PlayerAction.SURRENDER -> {
                "ERROR: SURRENDER not allowed in strategy table".toToastShort()
                action
            }

            else -> {
                action
            }
        }

        var bestAction = when {
            pHand.canSplit -> bestSplitAction()
            pHand.isSoft -> bestSoftAction()
            else -> bestHardAction()
        }

        // change combined actions to simple actions, and check no simple SURRENDER allowed
        bestAction = screenCombinedActions(bestAction)

        // now, we have only one of pure actions. screen once more. simple SURRENDER is OK now
        // check if DOUBLEDOWN is possible or not
        if ((bestAction == PlayerAction.DOUBLEDOWN) && !pHand.canDoubleDown) {
            if (pHand.canSplit || pHand.isSoft) {
                bestAction = bestHardAction()
                bestAction = screenCombinedActions(bestAction)
            }
            if (bestAction == PlayerAction.DOUBLEDOWN) {
                bestAction = PlayerAction.HIT
            }
        }

        return bestAction
    }

    private fun makeActionsFromCodes(numActions: Int, codes: Array<StringArray>): ArrayList<PlayerActionList> {

        if (codes.size != numActions || codes[0].size != COUNT_UP_SCORES) {
            "ERROR: wrong strategy action code array size".toToastShort()
        }

        val actions = ArrayList<PlayerActionList>()
        codes.forEach { upCodes ->
            val upActions = PlayerActionList()
            upCodes.forEach { code ->
                val action = PlayerAction.findByCode(code)
                upActions.add(action)
            }
            actions.add(upActions)
        }
        return actions
    }

    companion object {
        const val COUNT_UP_SCORES = 10       // 2,3,4, ..., 10, A
        const val COUNT_HARD_ACTIONS = 11    // 4-8, 9, ..., 16, 17, 18+
        const val COUNT_SOFT_ACTIONS = 8     // A2, A3, A4, A5, A6, A7, A8, A9
        const val COUNT_PAIR_ACTIONS = 10    // (2,2), (3,3), ..., (9,9), (10,10), (A,A)

        val basic = newBasic()
        val vegasStrip = newVegasStrip()
        val atlantic = newAtlantic()
        val novice = newNovice()
        val predefinedStrategies = arrayOf(basic, vegasStrip, atlantic, novice)

        var hardTitles = arrayOf("Hard 8-", "Hard 9", "Hard 10", "Hard 11", "Hard 12", "Hard 13", "Hard 14", "Hard 15", "Hard 16", "Hard 17", "Hard 18+")
        var softTitles = arrayOf("Soft 13", "Soft 14", "Soft 15", "Soft 16", "Soft 17", "Soft 18", "Soft 19", "Soft 20")
        var pairTitles = arrayOf("2-2", "3-3", "4-4", "5-5", "6-6", "7-7", "8-8", "9-9", "10-10", "A-A")

        /**
         * CLASSIC BLACKJACK GOLD STRATEGY CHART - Microgaming Software - 0.13% House Edge
         * (Same rules apply to SINGLE DECK BLACKJACK - Amaya software)
         * (Single deck, no peek, Dealer stand on soft 17, Multiple cards to split Aces, Double on hard 9,10,11 only)
         */

        private fun newBasic(): Strategy {
            // hard table = 11 by 10
            val hardActionCodes = arrayOf(  // [~10][~9]
                    arrayOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"), // ~8
                    arrayOf("H", "Dh", "Dh", "Dh", "Dh", "H", "H", "H", "H", "H"), // 9
                    arrayOf("Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "H", "H"), // 10
                    arrayOf("Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh", "Dh"), // 11
                    arrayOf("H", "H", "S", "S", "S", "H", "H", "H", "H", "H"), // 12
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 13
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 14
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "Rh", "Rh"), // 15
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "Rh", "Rh", "Rh"), // 16
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "Rs"), // 17
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")) // 18+

            // soft table = 8 by 10
            val softActionCodes = arrayOf(// [~7][~9]
                    arrayOf("H", "H", "H", "Dh", "Dh", "H", "H", "H", "H", "H"), // A2
                    arrayOf("H", "H", "H", "Dh", "Dh", "H", "H", "H", "H", "H"), // A3
                    arrayOf("H", "H", "Dh", "Dh", "Dh", "H", "H", "H", "H", "H"), // A4
                    arrayOf("H", "H", "Dh", "Dh", "Dh", "H", "H", "H", "H", "H"), // A5
                    arrayOf("H", "Dh", "Dh", "Dh", "Dh", "H", "H", "H", "H", "H"), // A6
                    arrayOf("Ds", "Ds", "Ds", "Ds", "Ds", "S", "S", "H", "H", "H"), // A7
                    arrayOf("S", "S", "S", "S", "Ds", "S", "S", "S", "S", "S"), // A8
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")) // A9

            // pair table = 10 by 10
            val pairActionCodes = arrayOf(// [~9][~9]
                    arrayOf("Ph", "Ph", "P", "P", "P", "P", "H", "H", "H", "H"), // 2,2
                    arrayOf("Ph", "Ph", "P", "P", "P", "P", "H", "H", "H", "H"), // 3,3
                    arrayOf("H", "H", "H", "Ph", "Ph", "H", "H", "H", "H", "H"), // 4,4
                    arrayOf("D", "D", "D", "D", "D", "D", "D", "D", "H", "H"), // 5,5 = hard 10
                    arrayOf("Ph", "P", "P", "P", "P", "H", "H", "H", "H", "H"), // 6,6
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "S", "H"), // 7,7
                    arrayOf("P", "P", "P", "P", "P", "P", "P", "P", "P", "Rp"), // 8,8
                    arrayOf("P", "P", "P", "P", "P", "S", "P", "P", "S", "S"), // 9,9
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // 10,10
                    arrayOf("P", "P", "P", "P", "P", "P", "P", "P", "P", "P")) // A,A

            return Strategy("4~8DSS17",
                    "4-8 Deck, Dealer Stands on Soft 17",
                    hardActionCodes, softActionCodes, pairActionCodes)
        }

        /**
         * VEGAS STRIP BLACKJACK STRATEGY CHART - Microgaming Software - 0.36% house edge
         * (4 decks, Dealer stands on soft 17, Hole Card - dealer peeks)
         */
        private fun newVegasStrip(): Strategy {
            // hard table = 11 by 10
            val hardActionCodes = arrayOf(  // [~10][~9]
                    arrayOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"), // 8-
                    arrayOf("H", "D", "D", "D", "D", "H", "H", "H", "H", "H"), // 9
                    arrayOf("D", "D", "D", "D", "D", "D", "D", "D", "H", "H"), // 10
                    arrayOf("D", "D", "D", "D", "D", "D", "D", "D", "D", "H"), // 11
                    arrayOf("H", "H", "S", "S", "S", "H", "H", "H", "H", "H"), // 12
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 13
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 14
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 15
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 16
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // 17
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")) // 18+

            // soft table = 8 by 10
            val softActionCodes = arrayOf(// [~7][~9]
                    arrayOf("H", "H", "H", "D", "D", "H", "H", "H", "H", "H"), // A2
                    arrayOf("H", "H", "H", "D", "D", "H", "H", "H", "H", "H"), // A3
                    arrayOf("H", "H", "D", "D", "D", "H", "H", "H", "H", "H"), // A4
                    arrayOf("H", "H", "D", "D", "D", "H", "H", "H", "H", "H"), // A5
                    arrayOf("H", "D", "D", "D", "D", "H", "H", "H", "H", "H"), // A6
                    arrayOf("S", "D", "D", "D", "D", "S", "S", "H", "H", "H"), // A7
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // A8
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")) // A9

            // pair table = 10 by 10
            val pairActionCodes = arrayOf(// [~9][~9]
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"), // 2,2
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"), // 3,3
                    arrayOf("H", "H", "H", "P", "P", "H", "H", "H", "H", "H"), // 4,4
                    arrayOf("D", "D", "D", "D", "D", "D", "D", "D", "H", "H"), // 5,5 = hard 10
                    arrayOf("P", "P", "P", "P", "P", "H", "H", "H", "H", "H"), // 6,6
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"), // 7,7
                    arrayOf("P", "P", "P", "P", "P", "P", "P", "P", "P", "P"), // 8,8
                    arrayOf("P", "P", "P", "P", "P", "S", "P", "P", "S", "S"), // 9,9
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // 10,10
                    arrayOf("P", "P", "P", "P", "P", "P", "P", "P", "P", "P")) // A,A


            return Strategy("Vegas Strip",
                    "4 decks, Dealer stands on soft 17, Hole Card - dealer peeks",
                    hardActionCodes, softActionCodes, pairActionCodes)
        }

        /**
         * ATLANTIC CITY BLACKJACK STRATEGY CHART - Microgaming Software - 0.36% house edge
         * (8 decks, Surrender option, Hole Card, Dealer must stand on Soft 17)
         */

        private fun newAtlantic(): Strategy {
            // hard table = 11 by 10
            val hardActionCodes = arrayOf(  // [~10][~9]
                    arrayOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"), // 8-
                    arrayOf("H", "D", "D", "D", "D", "H", "H", "H", "H", "H"), // 9
                    arrayOf("D", "D", "D", "D", "D", "D", "D", "D", "H", "H"), // 10
                    arrayOf("D", "D", "D", "D", "D", "D", "D", "D", "D", "H"), // 11
                    arrayOf("H", "H", "S", "S", "S", "H", "H", "H", "H", "H"), // 12
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 13
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 14
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "R", "H"), // 15
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "R", "R", "R"), // 16
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // 17
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")) // 18+

            // soft table = 8 by 10
            val softActionCodes = arrayOf(// [~7][~9]
                    arrayOf("H", "H", "H", "D", "D", "H", "H", "H", "H", "H"), // A2
                    arrayOf("H", "H", "H", "D", "D", "H", "H", "H", "H", "H"), // A3
                    arrayOf("H", "H", "D", "D", "D", "H", "H", "H", "H", "H"), // A4
                    arrayOf("H", "H", "D", "D", "D", "H", "H", "H", "H", "H"), // A5
                    arrayOf("H", "D", "D", "D", "D", "H", "H", "H", "H", "H"), // A6
                    arrayOf("S", "D", "D", "D", "D", "S", "S", "H", "H", "H"), // A7
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // A8
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")) // A9

            // pair table = 10 by 10
            val pairActionCodes = arrayOf(// [~9][~9]
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"), // 2,2
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"), // 3,3
                    arrayOf("H", "H", "H", "P", "P", "H", "H", "H", "H", "H"), // 4,4
                    arrayOf("D", "D", "D", "D", "D", "D", "D", "D", "H", "H"), // 5,5 = hard 10
                    arrayOf("P", "P", "P", "P", "P", "H", "H", "H", "H", "H"), // 6,6
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"), // 7,7
                    arrayOf("P", "P", "P", "P", "P", "P", "P", "P", "P", "P"), // 8,8
                    arrayOf("P", "P", "P", "P", "P", "S", "P", "P", "S", "S"), // 9,9
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // 10,10
                    arrayOf("P", "P", "P", "P", "P", "P", "P", "P", "P", "P")) // A,A

            return Strategy("Atlantic",
                    "8 decks, Surrender option, Hole Card, Dealer must stand on Soft 17",
                    hardActionCodes, softActionCodes, pairActionCodes)
        }

        /**
         * TYPICAL NOVICE
         * (no hit on Hard 15-16, do doubledown on soft hand, stand on soft 17-18, no doubledown on high up-card)
         */
        private fun newNovice(): Strategy {
            // hard table = 11 by 10
            val hardActionCodes = arrayOf(  // [~10][~9]
                    arrayOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"), // 8-
                    arrayOf("H", "D", "D", "D", "D", "H", "H", "H", "H", "H"), // 9
                    arrayOf("D", "D", "D", "D", "D", "H", "H", "H", "H", "H"), // 10
                    arrayOf("D", "D", "D", "D", "D", "H", "H", "H", "H", "H"), // 11
                    arrayOf("H", "H", "S", "S", "S", "H", "H", "H", "H", "H"), // 12
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 13
                    arrayOf("S", "S", "S", "S", "S", "H", "H", "H", "H", "H"), // 14
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // 15
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // 16
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // 17
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")) // 18+

            // soft table = 8 by 10
            val softActionCodes = arrayOf(// [~7][~9]
                    arrayOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"), // A2
                    arrayOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"), // A3
                    arrayOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"), // A4
                    arrayOf("H", "H", "H", "H", "H", "H", "H", "H", "H", "H"), // A5
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // A6
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // A7
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // A8
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S")) // A9

            // pair table = 10 by 10
            val pairActionCodes = arrayOf(// [~9][~9]
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"), // 2,2
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"), // 3,3
                    arrayOf("H", "H", "H", "P", "P", "H", "H", "H", "H", "H"), // 4,4
                    arrayOf("D", "D", "D", "D", "D", "H", "H", "H", "H", "H"), // 5,5 = hard 10
                    arrayOf("P", "P", "P", "P", "P", "H", "H", "H", "H", "H"), // 6,6
                    arrayOf("P", "P", "P", "P", "P", "P", "H", "H", "H", "H"), // 7,7
                    arrayOf("P", "P", "P", "P", "P", "P", "P", "P", "P", "P"), // 8,8
                    arrayOf("P", "P", "P", "P", "P", "S", "P", "P", "S", "S"), // 9,9
                    arrayOf("S", "S", "S", "S", "S", "S", "S", "S", "S", "S"), // 10,10
                    arrayOf("P", "P", "P", "P", "P", "P", "P", "P", "P", "P")) // A,A

            return Strategy("Typical Novice",
                    "no hit on Hard 15-16, do doubledown on soft hand, stand on soft 17-18, no doubledown on high up-card",
                    hardActionCodes, softActionCodes, pairActionCodes)
        }
    }
}