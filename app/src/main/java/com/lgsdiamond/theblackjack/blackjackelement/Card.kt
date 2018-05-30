package com.lgsdiamond.theblackjack.blackjackelement

import com.lgsdiamond.theblackjack.BjCardView
import com.lgsdiamond.theblackjack.CardAnimation
import com.lgsdiamond.theblackjack.ClientAction
import java.util.*

enum class Suit {
    SPADE, DIAMOND, HEART, CLUB
}

enum class Rank {
    ACE, TWO, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK, QUEEN, KING
}

const val COUNT_CARD_IN_DECK = 52
const val COUNT_CARD_RANK = 13

class Card(private val suit: Suit, val rank: Rank) {
    lateinit var view: BjCardView     // will be initialize when dealing
    var delay: Int = 0                // animation delay, usually zero

    internal var hidden = false
    var whichAnimation: CardAnimation = CardAnimation.NONE

    private val number: Int
        get() = rank.ordinal + 1     // ACE =1, TWO=2, ..., KING=13
    val score: Int
        get() = if (number <= 10) number else 10
    val order: Int
        get() = suit.ordinal * COUNT_CARD_RANK + rank.ordinal

    override fun toString(): String = "$suit-$rank"
}

class Shoe(numDecks: Int, toBeRandom: Boolean) : ArrayList<Card>() {
    private val random: Random = if (toBeRandom) Random() else Random(DEFAULT_SHOE_RANDOM_SEED)
    private val countInitial: Int = numDecks * COUNT_CARD_IN_DECK
    private val cutPos: Int = (DEFAULT_CUT_CARD_POSITION * countInitial).toInt()

    var countDrawn: Int = 0
    val countMax: Int
        get() = cutPos
    val countRemaining: Int
        get() = cutPos - countDrawn
    var needShuffle: Boolean = false
        get() = countDrawn >= cutPos

    init {
        for (i in 0 until numDecks)
            addAll(Deck())

        shuffle()
    }


    override fun toString(): String {
        return ("$countDrawn /$size ($cutPos) -> ${cutPos - countDrawn} remaining")
    }

    //=== actions
    private fun shuffle() {
        for (i in size - 1 downTo 1) {
            val index = random.nextInt(i + 1)
            val temp = get(index)
            set(index, get(i))
            set(i, temp)
        }
        countDrawn = 0  // no card has been drawn
    }

    fun drawOneCard(): Card {
        if (countDrawn >= size) {
            // Emergency Shuffle
            shuffle()
        }
        if (countDrawn == cutPos) {
            BjService.broadcast(ClientAction.SHOE_CUT_CARD_DRAWN, null)
        }
        return get(countDrawn++)
    }

    fun readyRound() {
        if (needShuffle) {
            shuffle()
            BjService.broadcast(ClientAction.SHOE_SHUFFLE, null)
        }

        BjService.broadcast(ClientAction.SHOE_READY, null)
    }

    companion object {
        const val DEFAULT_CUT_CARD_POSITION = 0.9f  // default position for cut card
        const val DEFAULT_SHOE_RANDOM_SEED: Long = 100L
    }
}

private class Deck : ArrayList<Card>(COUNT_CARD_IN_DECK) {
    init {
        Suit.values().forEach { suit ->
            Rank.values().forEach { rank ->
                add(Card(suit, rank))
            }
        }
    }
}