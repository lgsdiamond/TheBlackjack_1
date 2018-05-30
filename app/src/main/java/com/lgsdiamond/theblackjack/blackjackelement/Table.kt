package com.lgsdiamond.theblackjack.blackjackelement

import com.lgsdiamond.theblackjack.ClientAction

enum class Stage { PENDING, BETTING, INITIAL_DEAL, OFFER_INSURANCE, PEEK_HOLE,
    DEAL_EACH_HAND, DEAL_DEALER, PAY_HANDS
}

/**
 * Created by LgsDi on 2018-03-10.
 */
class Table : ArrayList<Box>() {

    val dealer = Dealer()
    var currentStage: Stage = Stage.PENDING

    init {
        for (i in 1..tableRule.numBoxes) {  // from one
            add(Box(i))
        }
    }

    val dealerUpScore: Int
        get() = dealer.hand.upCardScore

    //======= STAGES (TOTAL 7 STAGES) ==========
    fun doStageBetting() {
        currentStage = Stage.BETTING

        dealer.readyRound()

        // initially playerHands are based on boxes
        forEach {
            it.readyRound()
            if (it.isNotEmpty()) dealer.playerHands.add(it[0]) // initial hand
        }

        BjService.broadcast(ClientAction.STAGE_START, Stage.BETTING)
    }

    fun doStageInitialDeal() {
        currentStage = Stage.INITIAL_DEAL

        // collect valid players
        var numHands = dealer.playerHands.size
        for (index in (numHands - 1) downTo 0) {
            val pHand = dealer.playerHands[index]
            val player = pHand.player
            val box = pHand.box

            if (player == ghostPlayer) {
                dealer.playerHands.removeAt(index)
            } else {
                if (box.bet >= Table.tableRule.minBet) {
                    pHand.bet = box.bet                 // bet is moved to player hand
                    box.better.skipRound = false        // let better know
                } else {
                    box.cancelBet()                     // put back the bet
                    dealer.playerHands.removeAt(index)
                    box.better.skipRound = true         // let better know
                }
            }
        }

        if (dealer.playerHands.isNotEmpty()) {
            BjService.broadcast(ClientAction.STAGE_START, Stage.INITIAL_DEAL)

            dealer.doInitialDeal()

            BjService.broadcast(ClientAction.STAGE_END, Stage.INITIAL_DEAL)

            when (dealer.hand.upCardScore) {
                1 -> {
                    doStageOfferInsurance()
                }
                10 -> {
                    doStagePeekHole()
                }
                else -> {
                    doStageDealEachHand()
                }
            }
        } else {
            BjService.broadcast(ClientAction.PROGRESS_MESSAGE, "No Player Hand to deal")
        }
    }

    fun doStagePeekHole() {
        if (!Table.gameRule.peekAllowed || (dealer.hand.size < 1)) {
            doStageDealEachHand()
            return
        }

        currentStage = Stage.PEEK_HOLE
        BjService.broadcast(ClientAction.STAGE_START, Stage.PEEK_HOLE)

        BjService.broadcast(ClientAction.CHECKING_BLACKJACK, null)

        val upCardScore = dealer.hand.upCardScore
        val hiddenCardScore = dealer.hand.hiddenCardScore
        if (((upCardScore == 10) && (hiddenCardScore == 1))
                || ((upCardScore == 1) && (hiddenCardScore == 10))) {
            BjService.broadcast(ClientAction.OPEN_HIDDEN_DEALER_CARD, null)

            BjService.broadcast(ClientAction.DEALER_BLACKJACK, null)

            doStagePayHands()
        } else {
            dealer.collectInsured()
            BjService.broadcast(ClientAction.DEALER_NO_BLACKJACK, null)

            // next stage move: peek hole -> deal each hand
            BjService.broadcast(ClientAction.STAGE_END, Stage.PEEK_HOLE)

            doStageDealEachHand()
        }
    }

    fun doStageOfferInsurance() {
        currentStage = Stage.OFFER_INSURANCE
        BjService.broadcast(ClientAction.STAGE_START, Stage.OFFER_INSURANCE)
    }

    fun doStageDealEachHand() {
        currentStage = Stage.DEAL_EACH_HAND
        BjService.broadcast(ClientAction.STAGE_START, currentStage)

        var needDeal = false
        for (hand in dealer.playerHands) {
            if (!hand.hasDealDone) {
                needDeal = true
                break
            }
        }

        if (needDeal) {
            dealer.currentHandIndex = 0
            dealer.dealEachPlayerHand()
        } else {
            // move to next stage, skipping deal_each_hand
            BjService.broadcast(ClientAction.STAGE_END, currentStage)
            doStageDealDealer()
        }
    }

    fun doStageDealDealer() {
        currentStage = Stage.DEAL_DEALER
        BjService.broadcast(ClientAction.STAGE_START, currentStage)


        dealer.dealDealerHand()

        // move next stage: deal dealer -> pay hands
        BjService.broadcast(ClientAction.STAGE_END, currentStage)
        doStagePayHands()
    }

    fun doStagePayHands() {
        currentStage = Stage.PAY_HANDS
        BjService.broadcast(ClientAction.STAGE_START, Stage.PAY_HANDS)

        dealer.payHands()
        updateBetters()

        writeStatus()

        BjService.broadcast(ClientAction.STAGE_END, Stage.PAY_HANDS)
    }
    //======= STAGES(DONE) ==========

    fun initialize() {

        Player.playerSelf.takeBox(this[0])
        Player.playerMartin.takeBox(this[1])
        Player.playerAntMartin.takeBox(this[2])
        Player.playerOscar.takeBox(this[3])

        // ready fresh table
        doStageBetting()
    }

    fun readyToDealAtLeast(): Boolean {    // at least one box ready
        val boxes = filter { it.playerSeated && (it.bet >= Table.tableRule.minBet) }
        return boxes.isNotEmpty()
    }

    fun boxBetPossible(position: Int, moreBet: Float): Boolean {
        val bet = this[position].bet
        val player = this[position].player
        return player.hasBalance(moreBet) && ((bet + moreBet) <= Table.tableRule.maxBet)
    }

    fun updateBetters() {
        val seatedBoxes = this.filter { it.playerSeated }
        val pHands = dealer.playerHands
        for (box in seatedBoxes) {
            val boxHands = pHands.filter { it.box == box }
            if (boxHands.isNotEmpty()) {
                var roundWin = 0.0f
                for (pHand in boxHands) {
                    roundWin += (pHand.winAmount - pHand.bet)
                }
                box.better?.reflectRoundResult(roundWin, box.bet)
            }
        }
    }


    fun writeStatus() {

        dealer.writeStatus()
        players.forEach { it.writeStatus() }
        gameRule.writeStatus()
        tableRule.writeStatus()
    }

    companion object {
        val gameRule = GameRule.sTestRule
        val tableRule = TableRule.sDefaultTableRule

        val ghostBetter = FlatBetter()
        val ghostPlayer = Player("Empty(Leave)", 0.0f)
        val ghostStrategy = Strategy.basic

        val players = Player.predefinedPlayers
        val strategies = Strategy.predefinedStrategies

        fun findPlayerByName(name: String): Player {
            for (player in players) {
                if (player.name == name) {
                    return player
                }
            }
            return ghostPlayer
        }
    }
}