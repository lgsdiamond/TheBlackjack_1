package com.lgsdiamond.theblackjack

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import com.lgsdiamond.theblackjack.blackjackelement.CardDealRule
import com.lgsdiamond.theblackjack.blackjackelement.DoubleDownRule
import com.lgsdiamond.theblackjack.blackjackelement.Table
import kotlinx.android.synthetic.main.content_preference.*
import kotlinx.android.synthetic.main.row_pref.view.*
import kotlinx.android.synthetic.main.row_pref_category.view.*
import kotlinx.android.synthetic.main.row_pref_spinner.view.*
import kotlinx.android.synthetic.main.row_pref_spinner_two.view.*
import kotlinx.android.synthetic.main.row_pref_switch.view.*
import kotlinx.android.synthetic.main.row_pref_text.view.*

class PreferenceFrag : BjFragment() {
    abstract inner class BjPref(val title: String) {
        abstract val description: String
        abstract val needUpdate: Boolean

        abstract fun commitChange()
        abstract val prefString: String

        open fun bindView(holderView: View) {
            holderView.tvPrefRowTitle.setTextColor(if (needUpdate) Color.BLUE else Color.BLACK)
            holderView.tvPrefRowTitle.text = title
            holderView.tvPrefRowDescription.text = description
        }
    }

    abstract inner class TextPref(title: String, private val textDesc: String, private val inText: String)
        : BjPref(title) {

        override val description
            get() = "$textDesc: $outText"
        override val needUpdate
            get() = (outText != inText)

        var outText: String = inText

        override fun bindView(holderView: View) {
            super.bindView(holderView)
            holderView.textPrefRow.text = outText
            holderView.textPrefRowChange.setOnClickListener({ v ->
                val builder = AlertDialog.Builder(gMainActivity)
                builder.setTitle("New \"$title\"")
                val input = BjEditText(gMainActivity)
                input.inputType = InputType.TYPE_CLASS_TEXT
                input.setText(outText)
                builder.setView(input)
                builder.setPositiveButton("OK".spanTitleFace()) { _, _ ->
                    outText = input.text.toString()
                    updatePreferencesText()
                    bindView(holderView)
                }
                builder.setNegativeButton("Cancel".spanTitleFace()) { dialog, _ ->
                    dialog.cancel()
                }

                builder.show()
            })
        }
    }

    abstract inner class SwitchPref(title: String, private val descriptionOFF: String,
                                    private val descriptionON: String,
                                    private val inChecked: Boolean, private val dataText: Array<String>)
        : BjPref(title) {
        override val description
            get() = if (outChecked) descriptionON else descriptionOFF
        override val needUpdate
            get() = (outChecked != inChecked)

        var outChecked = inChecked

        override fun bindView(holderView: View) {
            super.bindView(holderView)
            holderView.switchPrefRow.isChecked = outChecked

            holderView.switchPrefRow.textOff = dataText[0]
            holderView.switchPrefRow.textOn = dataText[1]

            holderView.switchPrefRow.setOnCheckedChangeListener({ _, isChecked ->
                outChecked = isChecked
                updatePreferencesText()
                bindView(holderView)
            })
        }
    }

    abstract inner class SpinnerPref(title: String, private val spinDesc: String,
                                     private val inPos: Int, val dataText: Array<String>)
        : BjPref(title) {
        override val description
            get() = "$spinDesc: ${dataText[outPos]}"
        override val needUpdate
            get() = (outPos != inPos)

        var outPos = inPos

        override fun bindView(holderView: View) {
            super.bindView(holderView)
            val dataAdapter = BjArrayAdapter(gMainActivity,
                    R.layout.simple_spinner_dropdown_item, dataText)
            dataAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            holderView.spinnerPrefRow.adapter = dataAdapter
            holderView.spinnerPrefRow.setSelection(outPos)

            holderView.spinnerPrefRow.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    outPos = position
                    updatePreferencesText()
                    bindView(holderView)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }

    abstract inner class SpinnerTwoPref(title: String, private val spinDesc1: String, inPos1: Int,
                                        private val spinDesc2: String, private var inPos2: Int,
                                        dataText1: Array<String>,
                                        private val dataText2: Array<String>)
        : SpinnerPref(title, spinDesc1, inPos1, dataText1) {
        private val description2
            get() = "$spinDesc2: ${dataText2[outPos2]}"
        override val needUpdate
            get() = (super.needUpdate || (outPos2 != inPos2))

        var outPos2 = inPos2

        override fun bindView(holderView: View) {
            super.bindView(holderView)

            val dataAdapter = BjArrayAdapter(gMainActivity,
                    R.layout.simple_spinner_dropdown_item, dataText2)
            dataAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            holderView.spinnerTwoPrefRow.adapter = dataAdapter
            holderView.spinnerTwoPrefRow.setSelection(outPos2)
            holderView.tvPrefRowDescription2.text = description2

            holderView.spinnerTwoPrefRow.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    outPos2 = position
                    updatePreferencesText()
                    bindView(holderView)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        }
    }

    inner class BjPrefCategory(private val categoryName: String) : ArrayList<BjPref>() {
        val prefStrings: String
            get() {
                var rString = ""
                for (pref in this) {
                    rString += "[${pref.prefString}] "
                }
                return rString
            }

        private var isFolded = false

        fun needUpdate(): Boolean {
            var needUpdate = false

            for (pref in this) {
                if (pref.needUpdate) {
                    needUpdate = true
                    break
                }
            }

            return needUpdate
        }

        fun bindView(holderView: View) {
            holderView.tvPrefRowCategoryTitle.typeface = FontUtility.contentFace
            holderView.tvPrefRowCategoryTitle.text = categoryName

            holderView.tvPrefRowCategoryTitle.setOnClickListener({ v ->
                if (isFolded) {
                    isFolded = false
                    holderView.prefRecycleView.visibility = View.VISIBLE
                } else {
                    isFolded = true
                    holderView.prefRecycleView.visibility = View.GONE
                }
            })
        }
    }

    inner class PrefFactory {
        // Dealer Hit on Soft 17: HS17:SS17
        // Surrender: Sur / N-Sur
        // Second Card Deal: 2ndCard=LATER/AtOnce
        // Peek Hole allowed? : PHole / N-PHole
        val dealerPrefCategory: BjPrefCategory
            get() {
                val dCat = BjPrefCategory("Dealer Options")

                val dealerName = object : TextPref("Dealer Name",
                        "dealer's name", Table.tableRule.dealerName) {

                    override val prefString: String
                        get() = "dealer=$outText"

                    override fun commitChange() {
                        Table.tableRule.dealerName = outText
                    }
                }

                dCat.add(dealerName)

                val dealerHitSoft17 = object : SwitchPref("Dealer hit on Soft 17?",
                        "dealer STANDS on Soft-17", "dealer HIT on Soft-17",
                        Table.gameRule.hitOnDealerSoft17, arrayOf("STAND", "HIT")) {

                    override val prefString: String
                        get() = if (outChecked) "HS17" else "SS17"

                    override fun commitChange() {
                        Table.gameRule.hitOnDealerSoft17 = outChecked
                    }
                }
                dCat.add(dealerHitSoft17)

                val dealerAllowSurrender = object : SwitchPref("Surrender is allowed for players?",
                        "player CAN NOT surrender", "player CAN surrender with first two cards",
                        Table.gameRule.surrenderAllowed, arrayOf("NO", "YES")) {

                    override val prefString: String
                        get() = if (outChecked) "Surrender" else "No_Surrender"

                    override fun commitChange() {
                        Table.gameRule.surrenderAllowed = outChecked
                    }
                }
                dCat.add(dealerAllowSurrender)

                val dealerDealSecondCard = object : SwitchPref("Second card dealt at once for Dealer hand and Split hand?",
                        "Second card for Dealer & Split hand will be dealt LATER",
                        "Second card for Dealer & Split hand will be dealt at ONCE",
                        Table.gameRule.cardDealRule == CardDealRule.DEAL_ONCE,
                        arrayOf("LATE", "ONCE")) {

                    override val prefString: String
                        get() = "2ndCard=" + if (outChecked) "ONCE" else "LATER"

                    override fun commitChange() {
                        Table.gameRule.cardDealRule = if (outChecked) CardDealRule.DEAL_ONCE else CardDealRule.DEAL_LATER
                    }
                }
                dCat.add(dealerDealSecondCard)

                val dealerAllowPeek = object : SwitchPref("Peek-hole allowed?",
                        "Dealer CAN NOT peek-hole for Ace or 10 up-card",
                        "Dealer CAN peek-hole for Ace or 10 up-card",
                        Table.gameRule.peekAllowed, arrayOf("NO", "YES")) {

                    override val prefString: String
                        get() = if (outChecked) "Peek" else "No_Peek"

                    override fun commitChange() {
                        Table.gameRule.peekAllowed = outChecked
                    }
                }
                dCat.add(dealerAllowPeek)

                return dCat
            }

        // Doubledown after Split: DaSp / N-DaSp
        // Doubledown on
        val doubledownPrefCategory: BjPrefCategory
            get() {
                val ddCat = BjPrefCategory("Doubledown Options")

                val doubledownAfterSplit = object : SwitchPref("Doubledown after Split allowed?",
                        "player CAN NOT doubledown after split",
                        "player CAN doubledown after split",
                        Table.gameRule.doubleAllowedAfterSplit, arrayOf("NO", "YES")) {

                    override val prefString: String
                        get() = if (outChecked) "DaSp" else "No_DaSp"

                    override fun commitChange() {
                        Table.gameRule.doubleAllowedAfterSplit = outChecked
                    }
                }
                ddCat.add(doubledownAfterSplit)

                val doubledownOn = object : SpinnerPref("Doubledown on which two cards?",
                        "Player can doubledown on", when (Table.gameRule.doubleDownRule) {
                    DoubleDownRule.NINE_TEN_ELEVEN -> 1
                    DoubleDownRule.TEN_ELEVEN -> 2
                    else -> 0
                }, arrayOf("Any 2 cards", "9/10/11", "10/11")) {

                    override val prefString: String
                        get() = dataText[outPos]

                    override fun commitChange() {
                        Table.gameRule.doubleDownRule = when (outPos) {
                            1 -> DoubleDownRule.NINE_TEN_ELEVEN
                            2 -> DoubleDownRule.TEN_ELEVEN
                            else -> DoubleDownRule.ANY_TWO
                        }
                    }
                }
                ddCat.add(doubledownOn)

                return ddCat
            }

        // Allow split for different 10 value cards: SD10 / N-SD10
        val splitPrefCategory: BjPrefCategory
            get() {
                val sPrefs = BjPrefCategory("Split Options")

                val splitMaximum = object : SpinnerPref("Maximum number of split",
                        "player can split upto",
                        when (Table.gameRule.maxSplitCount) {
                            1 -> 0
                            2 -> 1
                            3 -> 2
                            4 -> 3
                            else -> 4
                        }, arrayOf("1-Split", "2-Split", "3-Split", "4-Split", "all the time")) {

                    override fun commitChange() {
                        Table.gameRule.maxSplitCount = when (outPos) {
                            0 -> 1
                            1 -> 2
                            2 -> 3
                            3 -> 4
                            else -> Int.MAX_VALUE
                        }
                    }

                    override val prefString: String
                        get() = dataText[outPos]
                }
                sPrefs.add(splitMaximum)

                val allowAceResplit = object : SwitchPref("Allow re-split for Ace?",
                        "Player CAN NOT re-split Ace for split hand",
                        "Player CAN re-split Ace for split hand",
                        Table.gameRule.allowAceResplit, arrayOf("NO", "YES")) {

                    override fun commitChange() {
                        Table.gameRule.allowAceResplit = outChecked
                    }

                    override val prefString: String
                        get() = if (outChecked) "AceResplit" else "No_AceResplit"
                }
                sPrefs.add(allowAceResplit)

                val splitDifferentTen = object : SwitchPref("Allow split different 10 values cards?",
                        "Player CAN NOT split different 10 value cards",
                        "player CAN split different 10 value cards",
                        Table.gameRule.allowSplitDifferentTenValues, arrayOf("NO", "YES")) {

                    override fun commitChange() {
                        Table.gameRule.allowSplitDifferentTenValues = outChecked
                    }

                    override val prefString: String
                        get() = if (outChecked) "SpD10" else "No_SpD10"
                }
                sPrefs.add(splitDifferentTen)

                return sPrefs
            }

        val tablePrefCategory: BjPrefCategory
            get() {
                val tPrefs = BjPrefCategory("Table Options")

                // Number of Decks of Shoe: Deck=1 ~ Deck=8
                val shoeDeckCount = object : SpinnerPref("How many decks are used in Shoe",
                        "Number of decks in shoe",
                        when (Table.tableRule.numDecks) {
                            1 -> 0
                            2 -> 1
                            4 -> 2
                            6 -> 3
                            else -> 4
                        },
                        arrayOf("Single Deck", "Double Deck", "4 Decks", "6 Decks", "8 Decks")) {

                    override fun commitChange() {
                        Table.tableRule.numDecks = when (outPos) {
                            0 -> 1
                            1 -> 2
                            2 -> 4
                            3 -> 6
                            else -> 8
                        }
                    }

                    override val prefString: String
                        get() = "shoe=${dataText[outPos]}"
                }
                tPrefs.add(shoeDeckCount)

                // Max Number of Multi-Hand: Box-1 ~ Box-8
                val tableBoxCount = object : SpinnerPref("How many boxes for multi player hand?",
                        "Maximum number of multi-hand",
                        when {
                            (Table.tableRule.numBoxes <= 8) -> (Table.tableRule.numBoxes - 1)
                            else -> 8
                        },
                        arrayOf("1 Box", "2 Boxes", "3 Boxes", "4 Boxes", "5 Boxes", "6 Boxes", "7 Boxes", "8 Boxes", "20 Boxes")) {

                    override fun commitChange() {
                        Table.tableRule.numBoxes = when {
                            (outPos < 8) -> (outPos + 1)
                            else -> 20
                        }
                    }

                    override val prefString: String = "Boxes=${if (outPos < 8) (outPos + 1) else 20}"
                }
                tPrefs.add(tableBoxCount)

                val tableBlackjackPayout = object : SwitchPref("Blackjack Payout Ratio",
                        "Casino pays player's Blackjack with payout ratio: 6-5",
                        "Casino pays player's Blackjack with payout ratio: 3-2",
                        (Table.gameRule.blackjackRate == 1.5f), arrayOf("6-5", "3-2")) {

                    override fun commitChange() {
                        Table.tableRule.blackjackPayout = if (outChecked) 1.5f else 1.2f
                    }

                    override val prefString: String
                        get () = "BJ_Payout=${if (outChecked) "3-2" else "6-5"}"
                }
                tPrefs.add(tableBlackjackPayout)

                val tableMinBet = object : SpinnerPref("Minimum Bet",
                        "Minimum betting for each hand",
                        when (Table.tableRule.minBet) {
                            5.0f -> 1
                            10.0f -> 2
                            25.0f -> 3
                            else -> 0
                        }, arrayOf("$1", "$5", "$10", "$25")) {

                    override fun commitChange() {
                        Table.tableRule.minBet = when (outPos) {
                            1 -> 5.0f
                            2 -> 10.0f
                            3 -> 25.0f
                            else -> 1.0f
                        }
                    }

                    override val prefString: String
                        get() = "minBet=${dataText[outPos]}"
                }
                tPrefs.add(tableMinBet)

                val tableMaxBet = object : SpinnerPref("Maximum Bet",
                        "Maximum betting for each hand",
                        when (Table.tableRule.minBet) {
                            100.0f -> 0
                            1_000.0f -> 1
                            10_000.0f -> 2
                            else -> 3
                        }, arrayOf("$100", "$1,000", "$10,000", "No Limit")) {

                    override fun commitChange() {
                        Table.tableRule.maxBet = when (outPos) {
                            0 -> 100.0f
                            1 -> 1_000.0f
                            2 -> 10_000.0f
                            else -> Float.MAX_VALUE
                        }
                    }

                    override val prefString: String
                        get() = "maxBet=${dataText[outPos]}"
                }
                tPrefs.add(tableMaxBet)

                return tPrefs
            }

        val roundPrefCategory: BjPrefCategory
            get() {
                val rCat = BjPrefCategory("Other Options")

                val tablePlayerBankroll = object : SpinnerPref("Player's initial bankroll",
                        "Player starts with bankroll",
                        when (Table.tableRule.initBalance) {
                            100.0f -> 0
                            1_000.0f -> 1
                            10_000.0f -> 2
                            100_000.0f -> 3
                            1_000_000.0f -> 4
                            else -> 5
                        }, arrayOf("$100", "$1,000", "$10,000", "$100,000", "$1,000,000", "No-Limit")) {

                    override fun commitChange() {
                        Table.tableRule.initBalance = when (outPos) {
                            0 -> 100.0f
                            1 -> 1_000.0f
                            2 -> 10_000.0f
                            3 -> 100_000.0f
                            4 -> 1_000_000.0f
                            else -> Float.MAX_VALUE
                        }
                    }

                    override val prefString: String
                        get() = "iniTBalance=${dataText[outPos]}"
                }
                rCat.add(tablePlayerBankroll)

                val useRandomShoe = object : SwitchPref("Shoe Random Seed",
                        "Shoe uses NEW random seed at each launch",
                        "Shoe uses FIXED random seed at each launch",
                        Table.tableRule.fixedRandom, arrayOf("Random", "Fixed")) {

                    override fun commitChange() {
                        Table.tableRule.fixedRandom = outChecked
                    }

                    override val prefString: String
                        get() = "Shoe=${if (outChecked) "Fixed" else "New"}"
                }
                rCat.add(useRandomShoe)

                val useSound = object : SwitchPref("Play Sounds",
                        "During game, NO sound",
                        "During game, sound play OK",
                        Table.tableRule.useSound, arrayOf("OFF", "ON")) {

                    override fun commitChange() {
                        Table.tableRule.useSound = outChecked
                    }

                    override val prefString: String
                        get() = "Sound=${if (outChecked) "ON" else "OFF"}"
                }
                rCat.add(useSound)

                val useAnimation = object : SwitchPref("Animations",
                        "During game, NO animations",
                        "During game, animations are OK",
                        Table.tableRule.useAnimation, arrayOf("OFF", "ON")) {

                    override fun commitChange() {
                        Table.tableRule.useAnimation = outChecked
                    }

                    override val prefString: String
                        get() = "Animation=${if (outChecked) "ON" else "OFF"}"
                }
                rCat.add(useAnimation)

                return rCat
            }
    }

    private var mListener: CountingFrag.OnFragmentInteractionListener? = null

    private lateinit var prefFactory: PrefFactory
    private var totalPreference = ArrayList<BjPrefCategory>()

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is CountingFrag.OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            barTitle = arguments.getString(PreferenceFrag.BAR_TITLE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_preference, container, false)
    }

    override fun initFragmentUI(view: View) {
        if (totalPreference.isEmpty()) {
            prefFactory = PrefFactory()
            totalPreference.add(prefFactory.dealerPrefCategory)
            totalPreference.add(prefFactory.doubledownPrefCategory)
            totalPreference.add(prefFactory.splitPrefCategory)
            totalPreference.add(prefFactory.tablePrefCategory)
            totalPreference.add(prefFactory.roundPrefCategory)
        }

        btnSettingAccept.setOnClickListener({ onClickEnding(btnSettingAccept) })// TODO: add
        btnSettingCancel.setOnClickListener({ onClickEnding(btnSettingCancel) })    // TODO: add

        prefCategoryRecycleView.layoutManager = LinearLayoutManager(gMainActivity)
        prefCategoryRecycleView.adapter = BjPrefCategoryAdapter(totalPreference, null)

        // minor tuning
        tvRuleText_Preference.typeface = FontUtility.contentFace
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    private fun onClickEnding(v: View) {
        val needUpdate = needUpdate()

        when (v.id) {

            R.id.btnSettingAccept -> if (needUpdate) {
                val alertConfirm = AlertDialog.Builder(gMainActivity)
                alertConfirm.setMessage("Save changed settings?".spanContentFace()).setCancelable(false)
                        .setPositiveButton("Yes".spanTitleFace()) { _, _ ->
                            commitPrefChanges()
                            gMainActivity.onBackPressed()
                        }.setNegativeButton("Cancel".spanTitleFace(),
                                DialogInterface.OnClickListener { _, _ ->
                                    return@OnClickListener
                                })
                val alert = alertConfirm.create()
                alert.show()
            } else {
                gMainActivity.onBackPressed()
            }
            R.id.btnSettingCancel -> if (needUpdate) {
                val alertConfirm = AlertDialog.Builder(gMainActivity)
                alertConfirm.setMessage("Discard changed settings?".spanContentFace()).setCancelable(false)
                        .setPositiveButton("Yes".spanTitleFace()) { _, _
                            ->
                            gMainActivity.onBackPressed()
                        }
                        .setNegativeButton("Cancel".spanTitleFace(),
                                DialogInterface.OnClickListener { _, _ ->
                                    return@OnClickListener
                                })
                val alert = alertConfirm.create()
                alert.show()
            } else {
                gMainActivity.onBackPressed()
            }
        }
    }

    fun needUpdate(): Boolean {
        var needUpdate = false

        for (cat in totalPreference) {
            if (cat.needUpdate()) {
                needUpdate = true
                break
            }
        }

        return needUpdate
    }

    fun updatePreferencesText() {
        var pString = ""
        for (cat in totalPreference) {
            pString += cat.prefStrings
        }

        fun spanPrefStrings(): CharSequence {
            val span = SpannableString(pString)

            var pos = pString.indexOf('[', 0)
            while (pos >= 0) {
                span.setSpan(ForegroundColorSpan(Color.BLUE), pos, pos + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                pos = pString.indexOf('[', pos + 1)
            }

            pos = pString.indexOf(']', 0)
            while (pos >= 0) {
                span.setSpan(ForegroundColorSpan(Color.BLUE), pos, pos + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                pos = pString.indexOf(']', pos + 1)
            }
            return span
        }

        tvRuleText_Preference.text = spanPrefStrings()
    }

    //=== adding settings ===
    private fun commitPrefChanges() {
        for (cat in totalPreference) {
            for (pref in cat) {
                if (pref.needUpdate) pref.commitChange()
            }
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnListFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onListFragmentInteraction(item: BjPref)
    }

    override lateinit var barTitle: String

    companion object {

        // pref view type
        const val TYPE_TEXT = 0
        const val TYPE_SWITCH = 1
        const val TYPE_SPINNER = 2
        const val TYPE_SPINNER_TWO = 3

        private const val BAR_TITLE = "BAR_TITLE"

        fun newInstance(barTitle: String): PreferenceFrag {
            val fragment = PreferenceFrag()
            val args = Bundle()
            args.putString(BAR_TITLE, barTitle)
            fragment.arguments = args
            fragment.barTitle = barTitle

            return fragment
        }
    }

    inner class BjPrefCategoryAdapter(private val catList: List<BjPrefCategory>,
                                      private val mListener: PreferenceFrag.OnListFragmentInteractionListener?)
        : RecyclerView.Adapter<BjPrefCategoryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.row_pref_category, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val cat = catList[position]
            holder.mView.prefRecycleView.adapter = BjPrefViewAdapter(cat, null)
            cat.bindView(holder.mView)
        }

        override fun getItemCount(): Int {
            return catList.size
        }

        inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
            init {
            }
        }
    }

    inner class BjPrefViewAdapter(private val prefList: BjPrefCategory,
                                  private val mListener: PreferenceFrag.OnListFragmentInteractionListener?)
        : RecyclerView.Adapter<BjPrefViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val viewID = when (viewType) {
                1 -> R.layout.row_pref_switch
                2 -> R.layout.row_pref_spinner
                3 -> R.layout.row_pref_spinner_two
                else -> R.layout.row_pref_text
            }

            val view = LayoutInflater.from(parent.context)
                    .inflate(viewID, parent, false)

            return ViewHolder(view)
        }

        override fun getItemViewType(position: Int): Int = when (prefList[position]) {
            is SwitchPref -> PreferenceFrag.TYPE_SWITCH
            is SpinnerPref -> PreferenceFrag.TYPE_SPINNER
            is SpinnerTwoPref -> PreferenceFrag.TYPE_SPINNER_TWO
            else -> PreferenceFrag.TYPE_TEXT
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pref = prefList[position]

            holder.mView.tvPrefRowDescription.typeface = FontUtility.contentFace
            pref.bindView(holder.mView)

            holder.mView.setOnClickListener {
                mListener?.onListFragmentInteraction(pref)
            }
        }

        override fun getItemCount(): Int {
            return prefList.size
        }

        inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
            init {
            }
        }
    }
}
