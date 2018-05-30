package com.lgsdiamond.theblackjack.blackjackelement

import com.lgsdiamond.theblackjack.toDollarAmountFloor
import java.util.*

enum class RoundResult { PENDING, SURRENDER, LOST, PUSH, WIN, WIN_BJ }

/**
 * Created by lgsdiamond on 2015-10-02.
 */
abstract class Hand : ArrayList<Card>() {
    var handScore: Int = 0
    var isSoft: Boolean = false

    val isTwentyOne: Boolean
        get() = (handScore == 21)
    open val isBlackjack: Boolean
        get() = (size == 2) && (handScore == 21)
    val isBust: Boolean
        get() = (handScore > 21)

    open val scoreText: String
        get() = when {
            isBust -> "$handScore(BUST)"
            isBlackjack -> "Blackjack"
            isTwentyOne -> "21"
            isSoft -> "S$handScore"
            else -> "$handScore"
        }

    override fun toString(): String {
        var status = ""

        forEach { card ->
            status += if (card.hidden) "[H]" else if (card.score == 1) "[A]" else "[${card.score}]"
        }
        status += " => $scoreText"
        return status
    }

    fun canBeBlackjack(): Boolean = when {
        (size == 0 || isBlackjack) -> true
        (size > 2) -> false
        ((this[0].score != 1) && (this[0].score != 10)) -> false
        ((size == 1) || ((size == 2) && this[1].hidden)) -> true
        else -> false
    }

    // checking conditions
    open fun updateValue() {
        handScore = 0

        var hasAce = false
        forEach { card ->
            if (!card.hidden) {
                if (card.rank == Rank.ACE) hasAce = true
                handScore += card.score
            }
        }

        isSoft = false
        if (hasAce && handScore < 12) {
            isSoft = true
            handScore += 10
        }
    }

    // manipulation
    fun addCard(card: Card) {       // use this instead of add()
        add(card)
        updateValue()
    }

    fun removeLastCard(): Card {    // use this instead of remove()
        val lastCard = removeAt(size - 1)
        updateValue()
        return lastCard
    }

    fun clearCards() {              // use this instead of clear()
        clear()
        updateValue()
    }
}

//=== Dealer Hand ===
class DealerHand : Hand() {

    val upCardScore: Int
        get() = if (size > 0) this[0].score else 0
    val hiddenSecondCard: Boolean
        get() = ((size == 2) && this[1].hidden)
    val hiddenCardScore: Int
        get() = if (hiddenSecondCard) this[1].score else 0

    override val scoreText: String
        get() {
            if ((size == 1) || ((size == 2) && (this[1].hidden)))
                return "Up-" + if (upCardScore == 11) "ACE" else upCardScore.toString()
            return super.scoreText
        }

    fun openSecondCard() {
        this[1].hidden = false
        updateValue()
    }
}

//=== Player Hand ===
class PlayerHand(val box: Box, var bet: Float) : Hand() {
    var player: Player = if (box.playerSeated) box.player else Table.ghostPlayer
    var insured: Float = 0.0f
    var roundResult: RoundResult = RoundResult.PENDING
    var insurePaid: Float = 0.0f
    var winAmount: Float = 0.0f
    var splitCount: Int = 0

    val hasInsured
        get() = (insured > 0.0)

    val hasDealDone
        get() = isBust || isTwentyOne || (roundResult == RoundResult.SURRENDER)
                || (Table.gameRule.justOneCardAfterAceSplit
                && (splitCount > 0) && (size == 2) && (this[0].rank == Rank.ACE))

    val canSplit: Boolean
        get() {
            return ((size == 2) && player.hasBalance(bet)                   // two cards & has balance
                    && (this[0].score == this[1].score)                     // same score
                    && (splitCount < Table.gameRule.maxSplitCount)          // within allowed split count
                    && ((this[0].rank != Rank.ACE) || (splitCount < 1) ||   // Ace card, just once or allowed
                    Table.gameRule.moreThanOnceAceSplitAllowed))
        }

    val canDoubleDown: Boolean
        get() {
            var canDD = if (size == 2)
                when (Table.gameRule.doubleDownRule) {
                    DoubleDownRule.ANY_TWO -> player.hasBalance(bet)
                    DoubleDownRule.TEN_ELEVEN -> ((handScore == 10) || (handScore == 11))
                            && player.hasBalance(bet)
                    DoubleDownRule.NINE_TEN_ELEVEN -> (handScore > 8) && (handScore < 12)
                            && player.hasBalance(bet)
                }
            else false

            if (canDD) {
                if ((splitCount > 0) && !Table.gameRule.doubleAllowedAfterSplit)
                    canDD = false
            }

            return canDD
        }

    val canSurrender: Boolean
        get() = Table.gameRule.surrenderAllowed && (size == 2) && (splitCount < 1)

    override val isBlackjack: Boolean
        get() = (super.isBlackjack && (splitCount == 0))

    fun makeSplitHand(): PlayerHand {
        splitCount++

        player.takeOutBalance(bet)
        val splitCard = removeLastCard()
        val splitHand = PlayerHand(box, bet)

        splitHand.addCard(splitCard)
        splitHand.splitCount = splitCount

        return splitHand
    }

    fun doubleDownBet() {
        player.takeOutBalance(bet)
        bet += bet
    }

    fun takeInsurance() {
        insured = (bet * 0.5f).toDollarAmountFloor()
        if (player.hasBalance(insured)) {
            player.takeOutBalance(insured)
        } else {
            insured = 0.0f
        }
    }

    fun takeBackInsurance() {
        player.putInBalance(insured)
        insured = 0.0f
    }
}