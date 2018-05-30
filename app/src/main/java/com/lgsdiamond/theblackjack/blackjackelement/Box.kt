package com.lgsdiamond.theblackjack.blackjackelement

import com.lgsdiamond.theblackjack.*

/**
 * Created by LgsDi on 2018-03-10.
 */

class Box(val index: Int) : ArrayList<PlayerHand>() {
    var player: Player = Table.ghostPlayer
    var better: Better = Table.ghostBetter

    val playerSeated: Boolean
        get() {
            return player != Table.ghostPlayer
        }

    var bet: Float = 0.0f               // betting for current round

    override fun toString(): String {
        return "Box($index)"
    }

    fun readyRound() {
        clear()
        bet = 0.0f

        if (playerSeated) {         // keep the player
            val nextBet = better.calcNextBet().toValidBet()

            if (player.hasBalance(nextBet)) {
                addInitialBet(nextBet)
            } else {
                addInitialBet(player.balance.toAllinBet())   // all-in
            }
            add(PlayerHand(this, nextBet))
        } else {
            better = Table.ghostBetter
        }
    }

    fun addInitialBet(betAmount: Float) {
        if (playerSeated) {
            player.takeOutBalance(betAmount)
            bet += betAmount
        } else {
            "ERROR: Betting with no player".toToastShort()
        }
    }

    fun cancelBet() {
        if (playerSeated) {
            player.putInBalance(bet)
            bet = 0.0f
        } else {
            BjService.broadcast(ClientAction.PROGRESS_MESSAGE, "ERROR: Betting with no player") // TODO-error checking
        }
    }
}