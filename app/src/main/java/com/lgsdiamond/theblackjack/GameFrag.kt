package com.lgsdiamond.theblackjack

import android.app.Fragment
import android.content.*
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.LinearSmoothScroller
import android.support.v7.widget.RecyclerView
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.PopupMenu
import com.lgsdiamond.theblackjack.blackjackelement.*
import com.lgsdiamond.theblackjack.blackjackelement.BjService.Companion.clearDelayUI
import com.lgsdiamond.theblackjack.blackjackelement.BjService.Companion.notifyDelayUI
import kotlinx.android.extensions.LayoutContainer
import kotlinx.android.synthetic.main.fragment_game.*
import kotlinx.android.synthetic.main.player_hand_list.*


enum class ClientAction { STAGE_START, STAGE_END,
    PROGRESS_MESSAGE,
    DEAL_ONE_CARD, DEAL_EACH_HAND,
    CHECKING_BLACKJACK, DEALER_BLACKJACK, DEALER_NO_BLACKJACK,
    OPEN_HIDDEN_DEALER_CARD,
    SHOE_CUT_CARD_DRAWN, SHOE_SHUFFLE, SHOE_READY,
    SPLIT_CARD,
    EACH_HAND_PAY_DONE, PLAYER_BLACKJACK,
    REMOVE_USED_CARDS
}

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [GameFrag.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [GameFrag.newInstance] factory method to
 * create an instance of this fragment.
 */
class GameFrag : BjFragment() {

    private inner class BjReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (ClientAction.valueOf(intent.action)) {
                ClientAction.STAGE_START -> {
                    val stageIndex = intent.getIntExtra(IntentKey.KEY_STAGE.toString(), 0)
                    val stage = Stage.values()[stageIndex]
                    actionStageStart(stage)
                }

                ClientAction.STAGE_END -> {
                    val stageIndex = intent.getIntExtra(IntentKey.KEY_STAGE.toString(), 0)
                    val stage = Stage.values()[stageIndex]
                    actionStageEnd(stage)
                }

                ClientAction.PROGRESS_MESSAGE -> {
                    var msg = intent.getStringExtra(IntentKey.KEY_MSG_STRING.toString())
                    if (msg == null) msg = "No Message"
                    showProgressMsg(msg)
                    notifyDelayUI()
                }

                ClientAction.DEAL_ONE_CARD -> {
                    notifyDealerShoeCountChanged()

                    val dealerOrPlayer = intent.getStringExtra(IntentKey.KEY_DEALER_PLAYER.toString())
                    if (dealerOrPlayer == IntentKey.KEY_DEALER.toString()) {
                        val cardIndex = intent.getIntExtra(IntentKey.KEY_CARD_INDEX.toString(), 0)
                        val cardHidden = intent.getIntExtra(IntentKey.KEY_CARD_HIDDEN.toString(), 0)
                        val cardDelay = intent.getIntExtra(IntentKey.KEY_ANIM_DELAY.toString(), 0)

                        notifyDealerHandChanged()
                        val card = table.dealer.hand[cardIndex]
                        card.delay = cardDelay

                        animateDealerCard(cardIndex, cardHidden != 0)
                    } else {
                        val handIndex = intent.getIntExtra(IntentKey.KEY_HAND_INDEX.toString(), 0)
                        val cardIndex = intent.getIntExtra(IntentKey.KEY_CARD_INDEX.toString(), 0)
                        val cardDelay = intent.getIntExtra(IntentKey.KEY_ANIM_DELAY.toString(), 0)

                        scrollUpdateHandView(handIndex)

                        val card = table.dealer.playerHands[handIndex][cardIndex]
                        card.delay = cardDelay

                        animatePlayerCard(card)
                    }
                }

                ClientAction.PLAYER_BLACKJACK -> {
                    val handIndex = intent.getIntExtra(IntentKey.KEY_HAND_INDEX.toString(), 0)
                    animatePlayerBlackjack(handIndex)
                }

                ClientAction.DEAL_EACH_HAND -> {
                    val handIndex = intent.getIntExtra(IntentKey.KEY_HAND_INDEX.toString(), 0)

                    scrollUpdateHandView(handIndex)

                    val pHand = table.dealer.playerHands[handIndex]

                    val action = Table.ghostStrategy.consultBestAction(pHand, table.dealerUpScore)
                    val proposedButton = when (action) {
                        PlayerAction.SURRENDER -> {
                            btnPlaySurrender
                        }
                        PlayerAction.STAND -> {
                            btnPlayStand
                        }
                        PlayerAction.SPLIT -> {
                            btnPlaySplit
                        }
                        PlayerAction.HIT -> {
                            btnPlayHit
                        }
                        PlayerAction.DOUBLEDOWN -> {
                            btnPlayDoubleDown
                        }
                        else -> {
                            "ERROR: Wrong Strategy Action".toToastShort()
                            btnPlayStand    // default
                        }
                    }

                    btnPlaySplit.isEnabled = pHand.canSplit
                    btnPlayDoubleDown.isEnabled = pHand.canDoubleDown
                    btnPlaySurrender.visibility = if (pHand.canSurrender) View.VISIBLE else View.GONE

                    proposedButton.showProposed()
                    showProgressMsg("[$action] is proposed.")

                    // TODO: Test
                    if (pHand.player != Player.playerSelf) proposedButton.autoDelayedClick()

                    notifyDelayUI()
                }

                ClientAction.OPEN_HIDDEN_DEALER_CARD -> {
                    animateOpenHiddenCard()
                }

                ClientAction.SHOE_CUT_CARD_DRAWN -> {
                    animateShoeCutCardDrawn()
                }

                ClientAction.SHOE_SHUFFLE -> {
                    animateShoeShuffling()
                }

                ClientAction.SHOE_READY -> {
                    notifyDealerShoeCountChanged()
                    notifyDelayUI()
                }

                ClientAction.SPLIT_CARD -> {
                    val boxIndex = intent.getIntExtra(IntentKey.KEY_BOX_INDEX.toString(), 0)
                    val handIndex = intent.getIntExtra(IntentKey.KEY_HAND_INDEX.toString(), 0)

                    animateCardSplit(boxIndex, handIndex)
                }

                ClientAction.EACH_HAND_PAY_DONE -> {
                    val handIndex = intent.getIntExtra(IntentKey.KEY_HAND_INDEX.toString(), 0)

                    notifyDealerHandChanged()
                    scrollUpdateHandView(handIndex)
                    notifyDelayUI()
                }

                ClientAction.DEALER_BLACKJACK -> {
                    animateDealerBlackjack(true)
                }

                ClientAction.DEALER_NO_BLACKJACK -> {
                    animateDealerBlackjack(false)
                }

                ClientAction.REMOVE_USED_CARDS -> {
                    notifyDelayUI()
                }

                else -> {
                    "ERROR: Invalid Client Action".toToastShort()
                    notifyDelayUI()
                }
            }
        }
    }

    // client action for when the stage starts
    private fun actionStageStart(stage: Stage) {
        when (stage) {
            Stage.BETTING -> {
                showProgressMsg("SOUND: Place Bet Please!")

                showContinueButton(true, "Start\nDeal")

                scrollUpdateHandView(0)

                notifyDealerHandChanged()
                handAdapter.notifyDataSetChanged()

                notifyDelayUI()
            }

            Stage.INITIAL_DEAL -> {
                notifyDelayUI()
            }

            Stage.OFFER_INSURANCE -> {
                showProgressMsg("SOUND: any insurance?")
                showContinueButton(true, "Ins.\nDone")

                handAdapter.offeringInsurance = true

                handAdapter.notifyDataSetChanged()
                notifyDelayUI()
            }

            Stage.PEEK_HOLE -> {
                animatePeekHole()
                notifyDealerHandChanged()
            }

            Stage.DEAL_EACH_HAND -> {
                layoutActions.visibility = View.VISIBLE
                scrollUpdateHandView(0)

                notifyDelayUI()
            }

            Stage.DEAL_DEALER -> {
                notifyDelayUI()
            }

            Stage.PAY_HANDS -> {
                notifyDelayUI()
            }

            else -> {
                notifyDelayUI()
            }
        }
    }

    // client action for when the stage ends
    private fun actionStageEnd(stage: Stage) {
        when (stage) {
            Stage.BETTING -> {
                showProgressMsg("SOUND: No more bet")
                notifyDelayUI()
            }

            Stage.INITIAL_DEAL -> {
                notifyDelayUI()
            }

            Stage.OFFER_INSURANCE -> {
                notifyDelayUI()
            }

            Stage.PEEK_HOLE -> {
                handAdapter.notifyDataSetChanged()
                notifyDelayUI()
            }

            Stage.DEAL_EACH_HAND -> {
                layoutActions.visibility = View.GONE
                notifyDelayUI()

                startService(ServiceAction.DEAL_DEALER)
            }

            Stage.DEAL_DEALER -> {
                notifyDelayUI()
            }

            Stage.PAY_HANDS -> {
                scrollUpdateHandView(0)

                showContinueButton(true, "New\nRound")
                notifyDealerHandChanged()

                notifyDelayUI()
            }

            else -> {
                notifyDelayUI()
            }
        }
    }

    // table should be ready, right?
    private val table: Table = Table()

    private var mListener: OnFragmentInteractionListener? = null
    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            barTitle = arguments.getString(BAR_TITLE)
        }

        sFrag = this        // initializing companion data in the beginning

        // binding service
        doBindService()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        startService(ServiceAction.ROUND_READY)
    }

    fun startService(action: ServiceAction) {
        val intent = Intent(gMainActivity.applicationContext, BjService::class.java)
        intent.`package` = PACKAGE_NAME
        intent.action = action.toString()

        bjService.startService(intent)
    }

    private var isBound: Boolean = false
    lateinit var bjService: BjService

    private fun doBindService() {
        gMainActivity.bindService(Intent(gMainActivity, BjService::class.java), connection,
                Context.BIND_AUTO_CREATE)
        isBound = true
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            bjService = (service as BjService.LocalBinder).service // lateinit bjService here
            bjService.table = table         // lateinit bjService here

            // register client receiver
            val filter = IntentFilter()
            val actions = ClientAction.values()
            for (action in actions)
                filter.addAction(action.toString())

            LocalBroadcastManager.getInstance(gMainActivity)
                    .registerReceiver(BjReceiver(), filter)

            // just right after connection, initialize server
            startService(ServiceAction.INIT_SERVER)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_game, container, false)
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the gMainActivity and potentially other fragments contained in that
     * gMainActivity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    //====
    override fun initFragmentUI(view: View) {
        normalizeActionButtons()
        btnContinue.setOnClickListener({ _: View ->
            continueRound()
        })

        btnPlaySurrender.setOnClickListener({ _: View ->
            normalizeActionButtons()
            startService(ServiceAction.ACTION_SURRENDER)
        })

        btnPlayStand.setOnClickListener({ _: View ->
            normalizeActionButtons()
            startService(ServiceAction.ACTION_STAND)
        })

        btnPlayHit.setOnClickListener({ _: View ->
            normalizeActionButtons()
            startService(ServiceAction.ACTION_HIT)
        })

        btnPlaySplit.setOnClickListener({ _: View ->
            normalizeActionButtons()
            startService(ServiceAction.ACTION_SPLIT)
        })

        btnPlayDoubleDown.setOnClickListener({ _: View ->
            normalizeActionButtons()
            startService(ServiceAction.ACTION_DOUBLEDOWN)
        })

        btnContinue.visibility = View.GONE
        layoutActions.visibility = View.GONE

        initializeViewIDList()
        initializeAnimations()

        initializeHandListView()
        notifyDealerHandChanged()
    }

    // this should be called after onCreate
    private fun initializeHandListView() {
        handAdapter = HandAdapter(table.dealer.playerHands)
        player_hand_recyclerView.adapter = handAdapter

        handLayoutManager = HandLayoutManager(gMainActivity)
        player_hand_recyclerView.layoutManager = handLayoutManager
    }

    private fun insureCheckChanged(checkBox: CheckBox, pHand: PlayerHand, position: Int) {
        if (checkBox.isChecked) {
            pHand.takeInsurance()
        } else {
            pHand.takeBackInsurance()
        }

        handAdapter.notifyDataSetChanged()
    }

    // update betting UI based on current selected box
    private fun betPossible(position: Int, moreBet: Float): Boolean {
        return table.boxBetPossible(position, moreBet)
    }

    private fun continueRound() {
        when (table.currentStage) {
            Stage.BETTING -> {
                if (table.readyToDealAtLeast()) {
                    BjService.broadcast(ClientAction.STAGE_END, Stage.BETTING)
                    startService(ServiceAction.DEAL_INITIAL)
                } else {
                    BjService.broadcast(ClientAction.PROGRESS_MESSAGE, "No Eligible Box")
                }
            }
            Stage.OFFER_INSURANCE -> {
                doneOfferingInsurance()
                if (Table.gameRule.peekAllowed)
                    startService(ServiceAction.PEEK_HOLE)
            }
            Stage.PAY_HANDS -> {
                startService(ServiceAction.ROUND_READY)
            }
            else -> {
            }
        }

        showContinueButton(false)
    }

    fun showProgressMsg(msg: String) {
        txtProgress.text = msg
    }

    //=== testing
    private fun doneOfferingInsurance() {
        showProgressMsg("SOUND: Insurance closed")

        showContinueButton(false)

        handAdapter.offeringInsurance = false
        handAdapter.notifyDataSetChanged()

        startService(ServiceAction.PEEK_HOLE)
    }

    // Animations
    private fun animateCardSplit(boxIndex: Int, handIndex: Int) {
        notifyDelayUI()
    }

    private fun animateDealerCard(cardIndex: Int, toHidden: Boolean) {
        val card = table.dealer.hand[cardIndex]
        card.whichAnimation = CardAnimation.DEALING

        if (toHidden) {
            layoutDealerHand.removeView(dealer_card_02)
            layoutDealerHand.addView(dealer_card_02, 0)
        }

        notifyDealerHandChanged()
    }

    private fun animatePlayerCard(card: Card) {
        card.whichAnimation = CardAnimation.DEALING
    }

    private fun animatePeekHole() {
        table.dealer.hand[1].whichAnimation = CardAnimation.PEEKING
        notifyDealerHandChanged()
    }

    private fun animateOpenHiddenCard() {
        table.dealer.hand.openSecondCard()
        layoutDealerHand.removeView(dealer_card_02)
        layoutDealerHand.addView(dealer_card_02, 1)

        table.dealer.hand[1].whichAnimation = CardAnimation.OPENING
        notifyDealerHandChanged()
    }

    private fun animateShoeCutCardDrawn() {
        showProgressMsg("SOUND: Shoe Cut Card Drawn")
        notifyDelayUI()
    }

    private fun animateShoeShuffling() {
        showProgressMsg("SOUND: Shoe shuffling")
        notifyDelayUI()
    }

    private fun animateDealerBlackjack(toBlackjack: Boolean) {
        showProgressMsg("SOUND: Dealer has "
                + if (toBlackjack) "Blackjack!!" else "No Blackjack.")
        notifyDelayUI()
    }

    private fun animatePlayerBlackjack(index: Int) {
        showProgressMsg("SOUND: Player Hand[${index + 1}] has Blackjack")
        notifyDelayUI()
    }

    inner class HandLayoutManager(private val mContext: Context) : LinearLayoutManager(mContext) {

        override fun smoothScrollToPosition(recyclerView: RecyclerView,
                                            state: RecyclerView.State?, position: Int) {

            val smoothScroller = object : LinearSmoothScroller(mContext) {

                //This controls the direction in which smoothScroll looks
                //for your view
                override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                    return this@HandLayoutManager
                            .computeScrollVectorForPosition(targetPosition)
                }

                //This returns the milliseconds it takes to
                //scroll one pixel.
                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return MILLISECONDS_PER_INCH / displayMetrics.densityDpi
                }
            }

            smoothScroller.targetPosition = position
            startSmoothScroll(smoothScroller)
        }
    }

    private lateinit var handAdapter: HandAdapter
    private lateinit var handLayoutManager: HandLayoutManager

    abstract class AndroidExtensionsViewHolder(override val containerView: View)
        : RecyclerView.ViewHolder(containerView), LayoutContainer

    inner class HandAdapter(private val pHands: ArrayList<PlayerHand>) : RecyclerView.Adapter<HandAdapter.HandViewHolder>() {
        var offeringInsurance: Boolean = false
        val viewHolders = ArrayList<HandViewHolder>()

        inner class HandViewHolder(handView: View) : AndroidExtensionsViewHolder(handView) {
            init {
            }

            private fun updateBettingUI(position: Int) {

                btnBetOne.isEnabled = table.boxBetPossible(position, 1.0f)
                btnBetFive.isEnabled = table.boxBetPossible(position, 5.0f)
                btnBetTen.isEnabled = table.boxBetPossible(position, 10.0f)
                btnBetTwentyFive.isEnabled = table.boxBetPossible(position, 25.0f)
                btnBetHundred.isEnabled = table.boxBetPossible(position, 100.0f)

                btnBetOne.setOnClickListener({ v ->
                    addInitialBetClicked(v, position, 1.0f)
                })
                btnBetFive.setOnClickListener({ v ->
                    addInitialBetClicked(v, position, 5.0f)
                })
                btnBetTen.setOnClickListener({ v ->
                    addInitialBetClicked(v, position, 10.0f)
                })
                btnBetTwentyFive.setOnClickListener({ v ->
                    addInitialBetClicked(v, position, 25.0f)
                })
                btnBetHundred.setOnClickListener({ v ->
                    addInitialBetClicked(v, position, 100.0f)
                })

                btnCancelBet.setOnClickListener({ v ->
                    cancelBetClicked(v, position)
                })
            }

            private fun showPlayerCardViews(pHand: PlayerHand) {
                for (index in 0..(pHand.size - 1)) {
                    val cardView = itemView.findViewById<BjCardView>(playerCardViewID[index])
                    cardView.visibility = View.VISIBLE
                    pHand[index].view = cardView
                }

                for (index in pHand.size..12) {
                    val cardView = itemView.findViewById<ImageView>(playerCardViewID[index])
                    cardView.visibility = View.GONE
                }
                if (player_card_01.visibility == View.GONE) player_card_01.visibility = View.INVISIBLE


                for ((index, card) in pHand.iterator().withIndex()) {
                    val view = itemView.findViewById<ImageView>(playerCardViewID[index])
                    assignCardImage(view, card.order)
                }

                makeHandAnimation(pHand)

                if (pHand.size > 0) {
                    val lastCard = pHand.last()
                    if (lastCard.whichAnimation != CardAnimation.NONE) {
                        makeScoreAnimation(txtHandScore, lastCard.delay)
                    }
                }
            }

            fun bindBetting(position: Int) {
                val box = table[position]
                val bet = box.bet
                val player = box.player

                txtBettter.setOnClickListener({ v: View ->
                    adjustBetter(v, box)
                })
                if (bet >= Table.tableRule.minBet) {
                    itemView.setBackgroundColor(0x6FFFFFFF)
                } else {
                    itemView.setBackgroundColor(0x3FFFFFFF)
                }

                showPlayerCardViews(box[0])

                checkInsured.visibility = android.view.View.GONE
                txtInsured.visibility = android.view.View.GONE
                txtInsurePaid.visibility = android.view.View.GONE
                txtWinAmount.visibility = android.view.View.GONE
                txtRoundResult.visibility = android.view.View.GONE
                txtHandScore.visibility = android.view.View.GONE

                txtBoxNumber.text = "B${position + 1}"

                if (box.playerSeated) {
                    layoutChips.visibility = android.view.View.VISIBLE
                    updateBettingUI(position)
                } else {
                    layoutChips.visibility = android.view.View.GONE
                }

                txtPlayerName.setOnClickListener({ v ->
                    changeBoxPlayer(v, position)
                })

                if (box.playerSeated) {
                    txtBetAmount.visibility = android.view.View.VISIBLE
                    if (bet == 0.0f) {
                        val proposedBet = box.better.proposedBet()
                        txtBetAmount.text = "(${proposedBet.toDollarString()})"
                    } else {
                        txtBetAmount.text = bet.toDollarString()
                    }

                    txtPlayerName.text = player.name + "\n(${player.balance.toDollarString()})"
                } else {
                    txtBetAmount.visibility = android.view.View.INVISIBLE

                    txtPlayerName.text = player.name + "\n(None)"
                }
            }

            fun bindDealing(position: Int) {
                val pHand = pHands[position]
                val box = pHand.box
                val player = pHand.player
                val currentHand = table.dealer.currentHandIndex

                txtBettter.setOnClickListener(null)

                showPlayerCardViews(pHand)

                if (position == currentHand) {
                    itemView.setBackgroundColor(0x70FFFFFF)
                } else {
                    itemView.setBackgroundColor(0x20FFFFFF)
                }

                checkInsured.isChecked = pHand.hasInsured
                if (handAdapter.offeringInsurance) {
                    checkInsured.isEnabled = true
                    if (player.hasBalance((pHand.bet * 0.5f).toDollarAmountFloor())) {
                        if (pHand.isBlackjack) {
                            checkInsured.text = "Even Money?"
                        }
                        checkInsured.visibility = android.view.View.VISIBLE

                        checkInsured.setOnClickListener({ v ->
                            insureCheckChanged(v as CheckBox, pHand, position)
                        })
                    }
                } else {
                    checkInsured.setOnClickListener(null)
                    if (pHand.hasInsured) {
                        if (pHand.isBlackjack) {
                            checkInsured.text = "Even Money"
                        } else {
                            checkInsured.text = "Insured"
                        }
                        checkInsured.visibility = android.view.View.VISIBLE
                        checkInsured.isEnabled = false
                    } else {
                        checkInsured.visibility = android.view.View.GONE
                    }
                }

                if (pHand.insurePaid > 0.0) {
                    txtInsurePaid.visibility = View.VISIBLE
                    txtInsurePaid.text = "[${pHand.insurePaid.toDollarString()}]"
                } else {
                    txtInsurePaid.visibility = View.GONE
                }

                txtPlayerName.setOnClickListener(null)      // no player change during deal

                layoutChips.visibility = android.view.View.GONE

                txtHandScore.visibility = android.view.View.VISIBLE
                txtHandScore.text = pHand.scoreText

                val boxIndex = box.index
                txtBoxNumber.text = "B$boxIndex"
                if (box.size > 1) {
                    val handIndex = box.indexOf(pHand) + 1
                    txtBoxNumber.text = txtBoxNumber.text.toString() + "(H$handIndex)"
                }

                txtBetAmount.visibility = android.view.View.VISIBLE
                txtBetAmount.text = pHand.bet.toDollarString()

                txtPlayerName.visibility = android.view.View.VISIBLE
                txtPlayerName.text = player.name + "\n(${player.balance.toDollarString()})"

                txtBetAmount.text = pHand.bet.toDollarString()

                if (pHand.insured > 0.0) {
                    txtInsured.visibility = View.VISIBLE
                    txtInsured.text = "[${pHand.insured.toDollarString()}]"
                } else {
                    txtInsured.visibility = View.GONE
                    txtInsured.text = ""
                }

                if (pHand.roundResult == com.lgsdiamond.theblackjack.blackjackelement.RoundResult.PENDING) {
                    txtRoundResult.visibility = android.view.View.GONE
                    txtWinAmount.visibility = android.view.View.GONE
                } else {
                    txtRoundResult.visibility = android.view.View.VISIBLE
                    txtRoundResult.text = pHand.roundResult.toString()

                    txtWinAmount.visibility = android.view.View.VISIBLE
                    txtWinAmount.text = pHand.winAmount.toDollarString()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HandViewHolder {
            val handView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.player_hand_list, parent, false)
            return HandViewHolder(handView)
        }

        override fun getItemCount(): Int {
            return pHands.size
        }

        override fun onBindViewHolder(holder: HandViewHolder, position: Int) {
            if (table.currentStage == Stage.BETTING) {
                holder.bindBetting(position)
            } else {
                holder.bindDealing(position)
            }
        }

        fun adjustBetter(v: View, box: Box) {
            val better = box.better
        }

        fun changeBoxPlayer(v: View, position: Int) {
            val box = table[position]

            val playersPopup = PopupMenu(gMainActivity.application, v)
            val seatPlayer = box.player

            var playerChecked = false
            for (player in Table.players) {
                playersPopup.menu.add(player.name)
                if (seatPlayer == player) {
                    val size = playersPopup.menu.size()
                    val item = playersPopup.menu.getItem(size - 1)
                    item.isCheckable = true
                    item.isChecked = true
                    playerChecked = true
                }
            }
            playersPopup.menu.add(Table.ghostPlayer.name)

            if (!playerChecked) {
                val item = playersPopup.menu.getItem(playersPopup.menu.size() - 1)
                item.isCheckable = true
                item.isChecked = true
            }

            playersPopup.setOnMenuItemClickListener { menuItem ->
                val newPlayer = Table.findPlayerByName(menuItem.title.toString())
                newPlayer.takeBox(table[position])
                handAdapter.notifyDataSetChanged()
                true
            }

            FontUtility.customFaceMenu(playersPopup.menu, FontUtility.titleFace)
            playersPopup.show()
        }

        // treat betting boxes
        fun cancelBetClicked(v: View, position: Int) {
            table[position].cancelBet()
            handAdapter.notifyDataSetChanged()
        }

        fun addInitialBetClicked(v: View, position: Int, bet: Float) {
            table[position].addInitialBet(bet)
            handAdapter.notifyDataSetChanged()
        }
    }

    // for dealer update
    fun notifyDealerHandChanged() {
        val dealer = table.dealer
        val dHand = dealer.hand

        showDealerCardViews()
        if (table.currentStage == Stage.BETTING) {
            txtDealerScore.visibility = View.GONE
        } else {
            if (dHand.size > 0) {
                txtDealerScore.visibility = View.VISIBLE
                txtDealerScore.text = dealer.hand.scoreText
            } else {
                txtDealerScore.visibility = View.GONE
            }
        }
        txtDealerName.text = "Dealer\n(${dealer.balanceChange.toDollarString()})"

        notifyDealerShoeCountChanged()
    }

    fun notifyDealerShoeCountChanged() {
        val dealer = table.dealer
        shoeProgress.max = dealer.shoe.countMax
        shoeProgress.progress = dealer.shoe.countMax - dealer.shoe.countRemaining
    }

    fun showDealerCardViews() {
        val dHand = table.dealer.hand

        for (index in 0..(dHand.size - 1)) {
            val cardView = layoutDealerHand.findViewById<BjCardView>(dealerCardViewID[index])
            cardView.visibility = View.VISIBLE
            dHand[index].view = cardView
        }
        for (index in dHand.size..12) {
            val cardView = layoutDealerHand.findViewById<ImageView>(dealerCardViewID[index])
            cardView.visibility = View.GONE
        }

        if (dealer_card_01.visibility == View.GONE) dealer_card_01.visibility = View.INVISIBLE

        for ((index, card) in dHand.iterator().withIndex()) {
            val view = layoutDealerHand.findViewById<ImageView>(dealerCardViewID[index])
            assignCardImage(view, card.order, card.hidden)
        }

        makeHandAnimation(dHand)
        if (dHand.size > 0) {
            val lastCard = dHand.last()
            if (lastCard.whichAnimation != CardAnimation.NONE) {
                if (!lastCard.hidden)
                    makeScoreAnimation(txtDealerScore, lastCard.delay)
            }
        }
    }

    fun showContinueButton(toShow: Boolean, label: String = "") {
        if (toShow) {
            btnContinue.visibility = View.VISIBLE
            btnContinue.text = label
        } else {
            btnContinue.visibility = View.GONE
        }
    }

    fun getImageId(imageName: String): Int {
        return gMainActivity.resources.getIdentifier("drawable/$imageName", null,
                gMainActivity.packageName)
    }

    private fun getImageNameFromOrder(cardOrder: Int): String {
        val suitIndex = cardOrder / COUNT_CARD_RANK
        val rankIndex = cardOrder - (COUNT_CARD_RANK * suitIndex)

        val suitName = Suit.values()[suitIndex].toString()
        val rankName = Rank.values()[rankIndex].toString()

        return "${rankName.toLowerCase()}_of_${suitName.toLowerCase()}s"
    }

    private val cardImageBack = "card_back"
    fun getNewCardView(cardOrder: Int, toHidden: Boolean = false): ImageView {
        val view = ImageView(gMainActivity)
        assignCardImage(view, cardOrder, toHidden)

        return view
    }

    fun assignCardImage(view: ImageView, cardOrder: Int, toHidden: Boolean = false) {
        val imageName = if (toHidden) cardImageBack else getImageNameFromOrder(cardOrder)
        val imageId = getImageId(imageName)
        view.setImageResource(imageId)
    }

    fun scrollUpdateHandView(handIndex: Int) {
        var newIndex = if (handIndex <= 2) 0 else (handIndex + 1)
        if (newIndex >= handAdapter.itemCount) {
            newIndex = handAdapter.itemCount - 1
        }

        player_hand_recyclerView.smoothScrollToPosition(newIndex)

        handAdapter.notifyDataSetChanged()
    }

    override lateinit var barTitle: String

    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param BAR_TITLE Parameter 1.
         * @return A new instance of fragment BettingFrag.
         */

        private const val BAR_TITLE = "BAR_TITLE"
        const val MILLISECONDS_PER_INCH = 200f

        fun newInstance(barTitle: String): GameFrag {
            val fragment = GameFrag()
            val args = Bundle()
            args.putString(BAR_TITLE, barTitle)
            fragment.arguments = args
            fragment.barTitle = barTitle
            return fragment
        }

        lateinit var sFrag: GameFrag
        val playerCardViewID: ArrayList<Int> = ArrayList()
        val dealerCardViewID: ArrayList<Int> = ArrayList()

        private fun initializeViewIDList() {
            if (playerCardViewID.size == 0) {
                playerCardViewID.add(R.id.player_card_01)
                playerCardViewID.add(R.id.player_card_02)
                playerCardViewID.add(R.id.player_card_03)
                playerCardViewID.add(R.id.player_card_04)
                playerCardViewID.add(R.id.player_card_05)
                playerCardViewID.add(R.id.player_card_06)
                playerCardViewID.add(R.id.player_card_07)
                playerCardViewID.add(R.id.player_card_08)
                playerCardViewID.add(R.id.player_card_09)
                playerCardViewID.add(R.id.player_card_10)
                playerCardViewID.add(R.id.player_card_11)
                playerCardViewID.add(R.id.player_card_12)
                playerCardViewID.add(R.id.player_card_13)
            }

            if (dealerCardViewID.size == 0) {
                dealerCardViewID.add(R.id.dealer_card_01)
                dealerCardViewID.add(R.id.dealer_card_02)
                dealerCardViewID.add(R.id.dealer_card_03)
                dealerCardViewID.add(R.id.dealer_card_04)
                dealerCardViewID.add(R.id.dealer_card_05)
                dealerCardViewID.add(R.id.dealer_card_06)
                dealerCardViewID.add(R.id.dealer_card_07)
                dealerCardViewID.add(R.id.dealer_card_08)
                dealerCardViewID.add(R.id.dealer_card_09)
                dealerCardViewID.add(R.id.dealer_card_10)
                dealerCardViewID.add(R.id.dealer_card_11)
                dealerCardViewID.add(R.id.dealer_card_12)
                dealerCardViewID.add(R.id.dealer_card_13)
            }
        }
    }

    // animations
    private fun initializeAnimations() {
    }

    private fun makeScoreAnimation(scoreView: View, delay: Int) {
        val scoreAnim = BjAnimUtility.sScoreTextAnim
//        val scoreAnim = BjAnimUtility.newScoreTextAnimation()
        scoreAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {
            }

            override fun onAnimationEnd(animation: Animation?) {
                BjService.notifyDelayUI()
            }

            override fun onAnimationStart(animation: Animation?) {
            }
        })

        scoreAnim.startOffset = (delay * CARD_ANIMATION_DURATION * 1.5).toLong()

        scoreView.startAnimation(scoreAnim)
    }

    private fun makeHandAnimation(hand: Hand) {
        for (card in hand) {
            var cardAnim: AnimationSet? = null

            when (card.whichAnimation) {
                CardAnimation.DEALING -> {
                    cardAnim = BjAnimUtility.sCardDealAnim
//                    cardAnim = BjAnimUtility.newCardDealAnimation()
                    cardAnim.startOffset = (card.delay * CARD_ANIMATION_DURATION * 1.5).toLong()
                    card.view.animation = cardAnim
                }

                CardAnimation.PEEKING -> {
                    cardAnim = BjAnimUtility.sCardPeekAnim
//                    cardAnim = BjAnimUtility.newCardPeekAnimation()
                }

                CardAnimation.OPENING -> {
                    notifyDelayUI()
                }

                CardAnimation.DISCARDING -> {
                    cardAnim = BjAnimUtility.sCardDiscardAnim
//                    cardAnim = BjAnimUtility.newCardDiscardAnimation()
                }

                else -> {
                }
            }

            if (cardAnim != null) {
                cardAnim.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        notifyDelayUI()
                    }

                    override fun onAnimationStart(animation: Animation?) {
                        clearDelayUI()
                    }
                })
                card.view.startAnimation(cardAnim)

                // now, all card animation done
                card.whichAnimation = CardAnimation.NONE
            }
        }
    }

    // Button
    private fun normalizeActionButtons() {
        btnPlaySurrender.backToNormal()
        btnPlayStand.backToNormal()
        btnPlayHit.backToNormal()
        btnPlaySplit.backToNormal()
        btnPlayDoubleDown.backToNormal()
    }
}