package com.lgsdiamond.theblackjack.blackjackelement

abstract class Better {

    // all betters has common properties
    var skipRound: Boolean = false
    var prevBet: Float = 0.0f
    var baseBet: Float = Table.tableRule.minBet
    var prevResult: RoundResult = RoundResult.PENDING   // only PENDING, WIN, LOST, PUSH. no WIN_BJ
    var totalWin: Float = 0.0f

    init {
        reset()
    }

    open fun reset() {
        skipRound = false
        prevBet = 0.0f
        baseBet = Table.tableRule.minBet
        prevResult = RoundResult.PENDING
        totalWin = 0.0f
    }

    abstract val preference: BettingPreference

    /**
     * it covers starting or no previous bet, otherwise return negative bet
     */
    fun calcNextBet(): Float = when {
        skipRound -> 0.0f
        (prevResult == RoundResult.PENDING) -> baseBet
        else -> proposedBet()
    }

    abstract fun proposedBet(): Float

    /**
     * at the end of round, evaluate & reflect round result
     */
    fun reflectRoundResult(roundWin: Float, bet: Float) {
        totalWin += roundWin
        prevResult = when {
            (roundWin > 0.0f) -> RoundResult.WIN
            (roundWin == 0.0f) -> RoundResult.PUSH
            (roundWin < 0.0f) -> RoundResult.LOST
            else -> RoundResult.PENDING
        }
        prevBet = bet
    }

    companion object {
        fun makeNewBetter(pref: BettingPreference): Better {
            return when (pref) {
                BettingPreference.MARTINGALE -> MartingaleBetter()
                BettingPreference.ANTI_MARTINGALE -> AntiMartingaleBetter(0.5f, 5)
                BettingPreference.OSCAR -> OscarBetter()
                else -> FlatBetter()
            }
        }
    }
}

/**
 * Just one win covers all loss and win one baseBet
 */
class MartingaleBetter : Better() {
    override val preference: BettingPreference
        get() = BettingPreference.MARTINGALE

    override fun proposedBet(): Float {
        val need = baseBet - totalWin
        return when {
            (need <= 0.0f) -> {       // goal achieved
                reset()
                baseBet
            }
            else -> need
        }
    }
}

/**
 * When win, increase bet with rate of winning amount up to maxCount. When lost, return back to baseBet
 */
class AntiMartingaleBetter(private var increaseRate: Float, private var maxCount: Int) : Better() {
    override val preference: BettingPreference
        get() = BettingPreference.ANTI_MARTINGALE

    private var count = 0

    override fun reset() {
        super.reset()
        count = 0
    }

    override fun proposedBet(): Float {

        return when (prevResult) {
            RoundResult.PUSH -> {
                arrayOf(prevBet, baseBet).max()!!
            }
            RoundResult.WIN -> {
                if (++count > maxCount) {
                    reset()
                    baseBet
                } else {
                    baseBet + totalWin * increaseRate
                }
            }
            else -> {
                reset()
                baseBet
            }
        }
    }
}

/**
 * When win, increase bet with baseBet. When lost, stay previous bet. repeat until win one baseBet
 */
class OscarBetter : Better() {
    override val preference: BettingPreference
        get() = BettingPreference.OSCAR

    override fun proposedBet(): Float {
        val need = baseBet - totalWin
        return when {
            (need <= 0.0) -> {       // goal achieved
                reset()
                baseBet
            }
            (prevResult == RoundResult.WIN) -> prevBet + baseBet
            else -> arrayOf(prevBet, baseBet).max()!!
        }
    }
}

/**
 * Stay previous bet all the time
 */
class FlatBetter : Better() {
    override val preference: BettingPreference
        get() = BettingPreference.FLATTER

    override fun proposedBet(): Float {
        return arrayOf(baseBet, prevBet).max()!!
    }
}