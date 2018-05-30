package com.lgsdiamond.theblackjack.blackjackelement

import com.lgsdiamond.theblackjack.ClientAction

/**
 * Created by lgsdiamond on 2015-10-02.
 */
abstract class Person(var name: String, private val balanceInitial: Float = 0.0f) {
    var balance: Float = balanceInitial
    val balanceChange: Float
        get() = balance - balanceInitial

    //=== paying ===
    fun takeOutBalance(amount: Float) {
        balance -= amount
    }

    fun putInBalance(amount: Float) {
        balance += amount
    }

    fun resetBalance() {
        balance = balanceInitial
    }

    fun hasBalance(request: Float) = (balance >= request)
}

//=== Dealer ===
const val NO_CURRENT_HAND = -1

class Dealer : Person("Dealer") {
    override fun toString(): String {
        return "[$name] $hand"
    }

    val hand = DealerHand()
    val shoe = Shoe(Table.tableRule.numDecks, Table.tableRule.fixedRandom)

    val playerHands: ArrayList<PlayerHand> = ArrayList()
    var currentHandIndex: Int = NO_CURRENT_HAND  // no current hand yet
        set(value) {
            field = if ((value >= 0) && (value < playerHands.size)) value else NO_CURRENT_HAND
        }

    private fun dealNextPlayerHand() {
        currentHandIndex = when (currentHandIndex) {
            NO_CURRENT_HAND -> 0
            (playerHands.size - 1) -> NO_CURRENT_HAND
            else -> currentHandIndex + 1
        }

        dealEachPlayerHand()    // it is OK, even (currentHandIndex == NO_CURRENT_HAND)
    }

    //=== Paying ===
    private fun payBet(pHand: PlayerHand, payRate: Float = 1.0f) {      // 1.5 for Blackjack
        val payAmount = pHand.bet * payRate
        pHand.winAmount = pHand.bet + payAmount
        takeOutBalance(payAmount)
        pHand.player.putInBalance(pHand.winAmount)
    }

    private fun takeBet(pHand: PlayerHand) {
        val lostAmount = pHand.bet
        putInBalance(lostAmount)
    }

    private fun pushBet(pHand: PlayerHand) {
        pHand.winAmount = pHand.bet
        pHand.player.putInBalance(pHand.winAmount)
    }

    //=== Insurance ===
    private fun takeInsured(pHand: PlayerHand) {
        putInBalance(pHand.insured)
    }

    private fun payInsured(pHand: PlayerHand, insuranceRate: Float) {
        val payAmount = pHand.insured * insuranceRate
        pHand.insurePaid = pHand.insured + payAmount
        takeOutBalance(payAmount)
        pHand.player.putInBalance(pHand.insurePaid)
    }

    private fun takeSurrenderBet(pHand: PlayerHand) {
        val lostAmount = 0.5f * pHand.bet
        pHand.winAmount = pHand.bet - lostAmount
        putInBalance(lostAmount)
        pHand.player.putInBalance(pHand.winAmount)

        // result
        pHand.roundResult = RoundResult.SURRENDER
    }

    fun collectInsured() {
        for (pHand in playerHands) {
            if (pHand.insured > 0.0f) {
                takeInsured(pHand)
                pHand.insured = 0.0f
            }
        }
    }

    // Round Activity
    fun readyRound() {
        hand.clearCards()                   // clear own hand
        playerHands.clear()                 // delete previous player's hands
        shoe.readyRound()
    }

    // dealing card
    private fun dealOneCard(hand: Hand, toHidden: Boolean = false, delay: Int = 0) {
        val card = shoe.drawOneCard()

        card.hidden = toHidden
        hand.addCard(card)

        // UI
        val cardInfo = IntArray(5)
        if (hand is PlayerHand) {                               // player card info
            cardInfo[0] = 0                                    // for player = 0
            cardInfo[1] = playerHands.indexOf(hand)
            cardInfo[2] = hand.indexOf(card)
            cardInfo[3] = delay
        } else {    // it should be dealer hand                                                // dealer card info
            cardInfo[0] = 1                                    // for dealer = 0
            cardInfo[1] = hand.indexOf(card)
            cardInfo[2] = if (card.hidden) 1 else 0            // quick and dirty
            cardInfo[3] = delay
        }

        BjService.broadcast(ClientAction.DEAL_ONE_CARD, cardInfo)
    }

    // handle player actions

    fun handleSurrender() {
        if (currentHandIndex != NO_CURRENT_HAND) {
            val pHand = playerHands[currentHandIndex]
            takeSurrenderBet(pHand)          // bet is handled now for surrender

            pHand.roundResult = RoundResult.SURRENDER

            dealNextPlayerHand()
        }
    }

    fun handleStand() {
        dealNextPlayerHand()
    }

    fun handleHit() {
        if (currentHandIndex != NO_CURRENT_HAND) {
            val pHand = playerHands[currentHandIndex]
            dealOneCard(pHand)

            if (pHand.isBust) {
                pHand.roundResult = RoundResult.LOST
            } else if (pHand.isBlackjack && !hand.canBeBlackjack()) {
                pHand.roundResult = RoundResult.WIN_BJ
            }

            if (pHand.hasDealDone) {
                dealNextPlayerHand()
            } else {
                BjService.broadcast(ClientAction.DEAL_EACH_HAND, currentHandIndex)
            }
        }
    }

    fun handleSplit() {
        if (currentHandIndex != NO_CURRENT_HAND) {
            val pHand = playerHands[currentHandIndex]
            val splitHand = pHand.makeSplitHand()
            pHand.box.add(pHand.box.indexOf(pHand) + 1, splitHand)    // box has one more hand next to current hand
            playerHands.add(currentHandIndex + 1, splitHand)           // one more hand to deal at the next position

            val splitInfo = IntArray(2)
            splitInfo[0] = splitHand.box.index - 1                   // 1-based -> 0-based
            splitInfo[1] = splitHand.box.indexOf(splitHand)

            BjService.broadcast(ClientAction.SPLIT_CARD, splitInfo)

            dealOneCard(pHand)                              // for first split hand
            if (Table.gameRule.cardDealRule == CardDealRule.DEAL_ONCE) {
                dealOneCard(splitHand, false, 1)
            }

            if (pHand.hasDealDone) {
                dealNextPlayerHand()
            } else {
                BjService.broadcast(ClientAction.DEAL_EACH_HAND, currentHandIndex)
            }

            if ((pHand[0].score == 1) && Table.gameRule.justOneCardAfterAceSplit) {
            }

        }
    }

    fun handleDoubleDown() {
        if (currentHandIndex != NO_CURRENT_HAND) {
            val pHand = playerHands[currentHandIndex]
            pHand.doubleDownBet()
            dealOneCard(pHand)

            dealNextPlayerHand()
        }
    }

    // round sActivity
    fun doInitialDeal() {
        for (pHand in playerHands) {
            dealOneCard(pHand)
        }

        dealOneCard(hand)

        for ((index, pHand) in playerHands.iterator().withIndex()) {
            dealOneCard(pHand)
            if (pHand.isBlackjack) {
                BjService.broadcast(ClientAction.PLAYER_BLACKJACK, index)
            }
        }

        if (Table.gameRule.cardDealRule == CardDealRule.DEAL_ONCE)
            dealOneCard(hand, true)

        // evaluate player hand with only two cards for dealDone and winBlackjack
        for (pHand in playerHands) {
            if (pHand.isBlackjack && !hand.canBeBlackjack())
                pHand.roundResult = RoundResult.WIN_BJ
        }
    }

    fun dealEachPlayerHand() {
        if (currentHandIndex == NO_CURRENT_HAND) {
            BjService.broadcast(ClientAction.STAGE_END, Stage.DEAL_EACH_HAND)
            dealDealerHand()
        } else {
            val pHand = playerHands[currentHandIndex]

            while (pHand.size < 2) {        // it could be just one card for cardDealRule AT_LATER
                dealOneCard(pHand)
            }

            if (pHand.hasDealDone) {
                dealNextPlayerHand()
            } else {
                BjService.broadcast(ClientAction.DEAL_EACH_HAND, currentHandIndex)
            }
        }
    }

    fun dealDealerHand() {
        var hasInsured = false
        for (pHand in playerHands) {
            if (pHand.hasInsured) {
                hasInsured = true
                break
            }
        }

        var hasPending = false
        for (pHand in playerHands) {
            if (pHand.roundResult == RoundResult.PENDING) {
                hasPending = true
                break
            }
        }

        // treat insured or pending players
        if (hasInsured || hasPending) {
            BjService.broadcast(ClientAction.STAGE_START, Stage.DEAL_DEALER)

            var delay = 0
            // open second card or get one card to make 2 cards
            if (hand.size == 1) {       // it could be just one card, for DEAL_LATER
                dealOneCard(hand, false, delay++)       // deal one open-card
            } else if (hand.hiddenSecondCard) {       // second card could be hidden, for DEAL_ONCE
                hand.openSecondCard()

                // UI
                BjService.broadcast(ClientAction.OPEN_HIDDEN_DEALER_CARD, hand[1])
            }

            // deal more for any pending players
            if (hasPending) {
                while (hand.handScore < 17) {
                    dealOneCard(hand, false, delay++)
                }
                if ((hand.isSoft && (hand.handScore == 17)) && Table.gameRule.hitOnDealerSoft17) {
                    dealOneCard(hand, false, delay++)
                    while (hand.handScore < 17) {
                        dealOneCard(hand, false, delay++)
                    }
                }
            }
        }
    }

    fun payHands() {
        for ((handIndex, pHand) in playerHands.iterator().withIndex()) {

            // treat insured hands first
            if (pHand.hasInsured) {
                if (hand.isBlackjack) {
                    payInsured(pHand, 2.0f)      // insurance rate is always 2.0
                } else {
                    takeInsured(pHand)
                }
            }

            // treat hand's betting
            when (pHand.roundResult) {
                RoundResult.SURRENDER -> {
                    // already cleared
                }
                RoundResult.PENDING -> {
                    if (pHand.isBlackjack && hand.isBlackjack) {
                        pHand.roundResult = RoundResult.PUSH
                    } else if (pHand.isBlackjack) {
                        pHand.roundResult = RoundResult.WIN_BJ
                    } else if (hand.isBlackjack) {
                        pHand.roundResult = RoundResult.LOST
                    } else if (hand.isBust) {                       // check dealer-bust first
                        pHand.roundResult = RoundResult.WIN
                    } else if (pHand.handScore > hand.handScore) {
                        pHand.roundResult = RoundResult.WIN
                    } else if (pHand.handScore == hand.handScore) {
                        pHand.roundResult = RoundResult.PUSH
                    } else if (pHand.handScore < hand.handScore) {
                        pHand.roundResult = RoundResult.LOST
                    } else {
                        BjService.broadcast(ClientAction.PROGRESS_MESSAGE, "ERROR: RoundResult can not be determined") // TODO-testing
                    }
                }
                else -> {
                    // Do nothing, it should be LOST(BUST) or WIN_BJ already
                }
            }

            // pay based on roundResult
            when (pHand.roundResult) {
                RoundResult.WIN_BJ -> {
                    payBet(pHand, Table.gameRule.blackjackRate)  // TODO-Rule: DoubleRate
                }
                RoundResult.WIN -> {
                    payBet(pHand)
                }
                RoundResult.PUSH -> {
                    pushBet(pHand)
                }
                RoundResult.LOST -> {
                    takeBet(pHand)
                }
                else -> {
                    BjService.broadcast(ClientAction.PROGRESS_MESSAGE, "ERROR: RoundResult can not be determined") // TODO-testing
                }
            }
            BjService.broadcast(ClientAction.EACH_HAND_PAY_DONE, handIndex) // TODO-testing
        }
    }

    fun readStatus() {
    }

    fun writeStatus() {
    }
}

//=== Player ===
enum class BettingPreference { FLATTER, MARTINGALE, ANTI_MARTINGALE, OSCAR }

class Player(name: String, bankroll: Float, val preferredBetting: BettingPreference = BettingPreference.FLATTER) : Person(name, bankroll) {

    fun takeBox(box: Box) {
        if (box.player == this) return
        if (box.playerSeated) box.player.leaveBox(box)

        box.player = this
        box.better = Better.makeNewBetter(preferredBetting)
        BjService.broadcast(ClientAction.PROGRESS_MESSAGE,
                "$this has taken Box-${box.index}")
    }

    fun leaveBox(box: Box) {
        box.cancelBet()
        box.better = Table.ghostBetter

        BjService.broadcast(ClientAction.PROGRESS_MESSAGE,
                "$this left Box-${box.index}")
    }

    override fun toString(): String {
        return "Player[$name] balance=$balance"
    }

    fun readStatus() {

    }


    fun writeStatus() {

    }

    companion object {
        val playerSelf = Player("MySelf", Table.tableRule.initBalance)
        val playerFlatter = Player("Flatter", Table.tableRule.initBalance)
        val playerMartin = Player("Martin", Table.tableRule.initBalance, BettingPreference.MARTINGALE)
        val playerAntMartin = Player("AntMartin", Table.tableRule.initBalance, BettingPreference.ANTI_MARTINGALE)
        val playerOscar = Player("Oscar", Table.tableRule.initBalance, BettingPreference.OSCAR)
        val predefinedPlayers = arrayOf(playerSelf, playerFlatter, playerMartin, playerAntMartin, playerOscar)
    }
}