package com.lgsdiamond.theblackjack.blackjackelement

import android.app.IntentService
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.Looper
import android.support.v4.content.LocalBroadcastManager
import com.lgsdiamond.theblackjack.ClientAction
import com.lgsdiamond.theblackjack.gContext
import com.lgsdiamond.theblackjack.toToastShort

/**
 * Created by lgsdi on 2018-03-14.
 */

enum class ServiceAction { INIT_SERVER,
    ROUND_READY, DEAL_INITIAL, PEEK_HOLE, DEAL_DEALER,
    ACTION_SURRENDER, ACTION_STAND, ACTION_HIT, ACTION_SPLIT, ACTION_DOUBLEDOWN
}

class BjService : IntentService("Blackjack Service") {

    private val mBinder: IBinder = LocalBinder()
    private var isBound = false

    lateinit var table: Table           // will be init in onConnection in GameFrag

    override fun onBind(p0: Intent?): IBinder {
        isBound = true
        return mBinder
    }

    inner class LocalBinder : Binder() {    // note: "inner" is essential
        val service: BjService
            get() = this@BjService
    }

    init {
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            when (intent.action) {
                ServiceAction.INIT_SERVER.toString() -> {
                    table.initialize()
                }

                ServiceAction.ROUND_READY.toString() -> {
                    broadcast(ClientAction.REMOVE_USED_CARDS, null)
                    table.doStageBetting()
                }

                ServiceAction.DEAL_INITIAL.toString() -> {
                    table.doStageInitialDeal()
                }

                ServiceAction.PEEK_HOLE.toString() -> {
                    table.doStagePeekHole()
                }

                ServiceAction.DEAL_DEALER.toString() -> {
                    table.doStageDealDealer()
                }

            // Player Actions
                ServiceAction.ACTION_SURRENDER.toString() -> {
                    table.dealer.handleSurrender()
                }

                ServiceAction.ACTION_STAND.toString() -> {
                    table.dealer.handleStand()
                }

                ServiceAction.ACTION_SPLIT.toString() -> {
                    table.dealer.handleSplit()
                }

                ServiceAction.ACTION_HIT.toString() -> {
                    table.dealer.handleHit()
                }

                ServiceAction.ACTION_DOUBLEDOWN.toString() -> {
                    table.dealer.handleDoubleDown()
                }

                else -> {
                }
            }
        }
    }

    companion object {
        @Volatile
        private var delayUI: Long = 0L   // for checking UI stuff done

        fun broadcast(action: ClientAction, param1: Any?, param2: Any? = null) = synchronized(delayUI) {

            val intent = BjIntent(action, param1, param2)
            LocalBroadcastManager.getInstance(gContext).sendBroadcast(intent)

            if (Looper.myLooper() != Looper.getMainLooper()) {
                val startTime = System.currentTimeMillis()
                clearDelayUI()

                while (delayUI < 0L) {       // wait until UI stuff done
                    val nowTime = System.currentTimeMillis()
                    val timeDiff = nowTime - startTime
                    if (timeDiff >= 5000) {
                        "WARNING: Too long to wait for delayUI".toToastShort()
                        break
                    }
                }

                val delay = delayUI
                while (delay > 0L) {
                    val nowTime = System.currentTimeMillis()
                    val timeDiff = nowTime - startTime
                    if (timeDiff > delay)
                        break
                }
                clearDelayUI()
            }
        }

        fun notifyDelayUI(delay: Long = 0L) {
            delayUI = delay
        }

        fun clearDelayUI() {
            delayUI = -1L
        }
    }
}

enum class IntentKey() { KEY_STAGE,
    KEY_MSG_STRING,
    KEY_SPLIT, KEY_DOUBLE_DOWN,
    KEY_DEALER_PLAYER, KEY_DEALER, KEY_PLAYER,
    KEY_BOX_INDEX, KEY_HAND_INDEX, KEY_CARD_INDEX, KEY_CARD_ORDER, KEY_CARD_HIDDEN,
    KEY_ANIM_DELAY
}

class BjIntent(action: ClientAction, param1: Any?, param2: Any? = null) : Intent(action.toString()) {
    init {
        when (action) {
            (ClientAction.STAGE_START) -> {
                if (param1 is Stage) putExtra(IntentKey.KEY_STAGE.toString(), param1.ordinal)
            }
            ClientAction.STAGE_END -> {
                if (param1 is Stage) putExtra(IntentKey.KEY_STAGE.toString(), param1.ordinal)
            }

            ClientAction.PROGRESS_MESSAGE -> {
                if (param1 is String) putExtra(IntentKey.KEY_MSG_STRING.toString(), param1)
            }

            ClientAction.DEAL_ONE_CARD -> {
                if (param1 is IntArray) {
                    if (param1[0] == 0) {                              // means player hand(= 0)
                        putExtra(IntentKey.KEY_DEALER_PLAYER.toString(),
                                IntentKey.KEY_PLAYER.toString())
                        putExtra(IntentKey.KEY_HAND_INDEX.toString(), param1[1])
                        putExtra(IntentKey.KEY_CARD_INDEX.toString(), param1[2])
                        putExtra(IntentKey.KEY_ANIM_DELAY.toString(), param1[3])
                    } else if (param1[0] == 1) {                       // means dealer hand(= 1)
                        putExtra(IntentKey.KEY_DEALER_PLAYER.toString(),
                                IntentKey.KEY_DEALER.toString())
                        putExtra(IntentKey.KEY_CARD_INDEX.toString(), param1[1])
                        putExtra(IntentKey.KEY_CARD_HIDDEN.toString(), param1[2])
                        putExtra(IntentKey.KEY_ANIM_DELAY.toString(), param1[3])
                    }
                } else {
                    "ERROR: Deal card intent with not a card info".toToastShort()
                }
            }

            ClientAction.PLAYER_BLACKJACK -> {
                if (param1 is Int) {
                    putExtra(IntentKey.KEY_HAND_INDEX.toString(), param1)
                } else {
                    "ERROR: Invalid Blackjack Hand".toToastShort()
                }
            }

            ClientAction.DEAL_EACH_HAND -> {
                if (param1 is Int) {
                    putExtra(IntentKey.KEY_HAND_INDEX.toString(), param1)   // handIndex
                } else {
                    "ERROR: Deal Hand with not a PlayerHand".toToastShort()
                }
            }
            ClientAction.OPEN_HIDDEN_DEALER_CARD -> {
                if (param1 is Card) {
                    putExtra(IntentKey.KEY_CARD_ORDER.toString(), param1.order)
                } else {
                    "ERROR: Opening hidden card with not a Card".toToastShort()
                }
            }

            ClientAction.SPLIT_CARD -> {
                if (param1 is IntArray) {  // size = 2
                    putExtra(IntentKey.KEY_BOX_INDEX.toString(), param1[0])
                    putExtra(IntentKey.KEY_HAND_INDEX.toString(), param1[1])
                } else {
                    "ERROR: Split card with Not a correct Info".toToastShort()
                }
            }

            ClientAction.EACH_HAND_PAY_DONE -> {
                if (param1 is Int) {  // handIndex
                    putExtra(IntentKey.KEY_HAND_INDEX.toString(), param1)
                } else {
                    "ERROR: Pay_done with invalid handIndex".toToastShort()
                }
            }

            else -> {

            }
        }
    }
}