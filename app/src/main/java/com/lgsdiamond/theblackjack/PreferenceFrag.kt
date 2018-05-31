package com.lgsdiamond.theblackjack

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
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
import kotlinx.android.synthetic.main.row_pref_switch.view.*
import kotlinx.android.synthetic.main.row_pref_text.view.*


class PrefFactory {

    // subclasses
    abstract class BjPref(var title: String, var description: String) {
        var mTouched = false

        abstract fun needUpdate(): Boolean
        abstract fun settingRuleIn()
        abstract fun settingRuleOut()

        fun setTouched(touched: Boolean) {
            mTouched = touched
        }
    }

    abstract inner class TextPref(title: String, description: String, name: String)
        : BjPref(title, description) {
        var mOldName: String = name
        var mNewName: String = name
        lateinit var editText: BjEditText

        override fun settingRuleIn() {
            if (mTouched) {
                editText.setText(mNewName)
            } else {
                mNewName = editText.text.toString()
                editText.setText(mOldName)
                mTouched = true
            }
        }

        override fun needUpdate(): Boolean {
            return (mOldName.contentEquals(mNewName))
        }
    }

    abstract inner class SwitchPref(title: String, description: String, var dataText: Array<String>)
        : BjPref(title, description) {
        var mIsChecked: Boolean = false
        var mOldChecked: Boolean = false
        lateinit var switch: BjSwitch

        abstract val genuineCheckedValue: Boolean

        override fun settingRuleIn() {
            if (mTouched) {
                switch.isChecked = mIsChecked
            } else {
                mOldChecked = genuineCheckedValue
                mIsChecked = mOldChecked
                switch.isChecked = mOldChecked
                mTouched = true
            }
        }

        override fun needUpdate(): Boolean {
            return (mOldChecked != mIsChecked)
        }

        override fun toString() = getSubString(if (mTouched) mIsChecked else genuineCheckedValue)

        abstract fun getSubString(isChecked: Boolean): String

        fun postSwitchCheckedChange() {
            // no nothing now
        }
    }

    abstract inner class SpinnerPref(title: String, description: String, var dataText: Array<String>)
        : BjPref(title, description) {
        var mSelectedPosition: Int = 0
        var mOldPosition: Int = 0
        lateinit var spinner: BjSpinner

        abstract val genuineIndexValue: Int

        override fun settingRuleIn() {
            if (mTouched) {
                spinner.setSelection(mSelectedPosition)
            } else {
                val index = genuineIndexValue
                spinner.setSelection(index)
                mOldPosition = spinner.selectedItemPosition
                mSelectedPosition = mOldPosition
                mTouched = true
            }
        }

        override fun needUpdate(): Boolean {
            return (mOldPosition != mSelectedPosition)
        }

        fun postSpinnerItemSelected() {}
    }

    abstract inner class SpinnerTwoPref(title: String, description: String, dataText: Array<String>,
                                        var mDataStringsTwo: Array<String>)
        : SpinnerPref(title, description, dataText) {
        var mSelectedPositionTwo: Int = 0
        var mOldPositionTwo: Int = 0
        lateinit var spinnerTwo: BjSpinner

        abstract override val genuineIndexValue: Int

        abstract val genuineIndexValueTwo: Int

        override fun settingRuleIn() {
            val mOldTouched = mTouched
            super.settingRuleIn()

            if (mOldTouched) {
                spinnerTwo.setSelection(mSelectedPositionTwo)
            } else {
                val index = genuineIndexValueTwo
                spinnerTwo.setSelection(index)
                mOldPositionTwo = spinnerTwo.selectedItemPosition
                mSelectedPositionTwo = mOldPositionTwo
                mTouched = true
            }
        }

        override fun needUpdate(): Boolean {
            return ((super.needUpdate() || (mOldPositionTwo != mSelectedPositionTwo)))
        }

        fun postSpinnerTwoItemSelected() {
            // Do nothing now
        }
    }

    class BjPrefCategory(val categoryName: String) : ArrayList<BjPref>() {
        val ruleString: String
            get() {
                var rString = ""
                for (pref in this) {
                    rString += "[$pref] "
                }
                return rString
            }

        var isFolded = false

        fun needUpdate(): Boolean {
            var needUpdate = false

            for (pref in this) {
                if (pref.needUpdate()) {
                    needUpdate = true
                    break
                }
            }

            return needUpdate
        }
    }

    // Dealer Hit on Soft 17: HS17:SS17
    // Surrender: Sur / N-Sur
    // Second Card Deal: 2ndCard=LATER/AtOnce
    // Peek Hole allowed? : PHole / N-PHole
    val dealerPrefCategory: BjPrefCategory
        get() {
            val dCat = BjPrefCategory("Dealer Options")

            val dealerName = object : TextPref("Dealer Name",
                    "dealer's name", Table.tableRule.dealerName) {

                override fun settingRuleOut() {
                    Table.tableRule.dealerName = mNewName
                }

                override fun toString(): String {
                    return "dealer=$mNewName"
                }
            }

            val dealerHitSoft17 = object : SwitchPref("Dealer Hit on Soft 17?",
                    "dealer can hit or stand on Soft 17",
                    arrayOf("STAND", "HIT")) {

                override val genuineCheckedValue: Boolean
                    get() = Table.gameRule.hitOnDealerSoft17

                override fun settingRuleOut() {
                    Table.gameRule.hitOnDealerSoft17 = mIsChecked
                }

                override fun getSubString(isChecked: Boolean): String {
                    return if (isChecked) "HS17" else "SS17"
                }
            }
            dCat.add(dealerName)

            dCat.add(dealerHitSoft17)
            val dealerAllowSurrender = object : SwitchPref("Allow Surrender",
                    "player can surrender with first two cards",
                    arrayOf("NO", "YES")) {
                override val genuineCheckedValue: Boolean
                    get() = Table.gameRule.surrenderAllowed

                override fun settingRuleOut() {
                    Table.gameRule.surrenderAllowed = mIsChecked
                }

                override fun getSubString(isChecked: Boolean): String {
                    return if (isChecked) "Surrender" else "No_Surrender"
                }
            }
            dCat.add(dealerAllowSurrender)
            val dealerDealSecondCard = object : SwitchPref("Dealer/Split Hand second card deal",
                    "dealer can deal the second card of dealer hand or split hand at once or later",
                    arrayOf("ONCE", "LATER")) {
                override val genuineCheckedValue: Boolean
                    get() = (Table.gameRule.cardDealRule == CardDealRule.DEAL_LATE)

                override fun settingRuleOut() {
                    Table.gameRule.cardDealRule = if (mIsChecked)
                        CardDealRule.DEAL_LATE
                    else
                        CardDealRule.DEAL_ONCE
                }

                override fun getSubString(isChecked: Boolean): String {
                    return "2ndCard=" + if (isChecked) "LATER" else "ONCE"
                }
            }
            dCat.add(dealerDealSecondCard)
            val dealerAllowPeek = object : SwitchPref("Peek-hole allowed",
                    "Dealer can use peek-hole for Ace or 10 card",
                    arrayOf("NO", "YES")) {
                override val genuineCheckedValue: Boolean
                    get() = (Table.gameRule.peekAllowed)

                override fun settingRuleOut() {
                    Table.gameRule.peekAllowed = mIsChecked
                }

                override fun getSubString(isChecked: Boolean): String {
                    return if (isChecked) "Peek" else "No_Peek"
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
            val doubledownAfterSplit = object : SwitchPref("Doubledown after Split",
                    "player can doubledown after card splitting",
                    arrayOf("NO", "YES")) {
                override val genuineCheckedValue: Boolean
                    get() = (Table.gameRule.doubleAllowedAfterSplit)

                override fun settingRuleOut() {
                    Table.gameRule.doubleAllowedAfterSplit = mIsChecked
                }

                override fun getSubString(isChecked: Boolean): String {
                    return if (isChecked) "DaSp" else "No_DaSp"
                }
            }
            ddCat.add(doubledownAfterSplit)

            val doubledownOn = object : SpinnerPref("Doubledown on",
                    "Double down on any 2 cards, 9/10/11, or 10/11",
                    arrayOf("Any 2 cards", "9/10/11", "10/11")) {

                override val genuineIndexValue: Int
                    get() {
                        val index: Int
                        when (Table.gameRule.doubleDownRule) {
                            DoubleDownRule.ANY_TWO -> index = 0
                            DoubleDownRule.NINE_TEN_ELEVEN -> index = 1
                            DoubleDownRule.TEN_ELEVEN -> index = 2
                        }

                        return index
                    }

                override fun settingRuleOut() {
                    Table.gameRule.doubleDownRule = getRuleDoubledownByIndex(mSelectedPosition)
                }

                fun getRuleDoubledownByIndex(index: Int) = when (index) {
                    0 -> DoubleDownRule.ANY_TWO
                    1 -> DoubleDownRule.NINE_TEN_ELEVEN
                    2 -> DoubleDownRule.TEN_ELEVEN
                    else -> DoubleDownRule.ANY_TWO
                }

                override fun toString(): String {
                    val rule = if (mTouched)
                        getRuleDoubledownByIndex(mSelectedPosition)
                    else
                        Table.gameRule.doubleDownRule

                    return ("DD=" + (if (rule == DoubleDownRule.ANY_TWO)
                        "Any2"
                    else
                        if (rule === DoubleDownRule.NINE_TEN_ELEVEN)
                            "9/10/11"
                        else
                            "10/11"))
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
                    "player can split",
                    arrayOf("1-Split", "2-Split", "3-Split", "4-Split", "all the time")) {

                override val genuineIndexValue: Int
                    get() {
                        val index: Int
                        when (Table.gameRule.maxSplitCount) {
                            1, 2, 3, 4 -> index = Table.gameRule.maxSplitCount - 1
                            99 -> index = 4
                            else -> index = 4
                        }
                        return index
                    }

                override fun settingRuleOut() {
                    Table.gameRule.maxSplitCount = getMaxSplitByIndex(mSelectedPosition)
                }

                fun getMaxSplitByIndex(index: Int): Int {
                    val maxSplit: Int
                    when (index) {
                        0, 1, 2, 3 -> maxSplit = index + 1
                        4 -> maxSplit = 99
                        else -> maxSplit = 4
                    }
                    return maxSplit
                }

                override fun toString(): String {
                    val maxSplit = if (mTouched)
                        getMaxSplitByIndex(mSelectedPosition)
                    else
                        Table.gameRule.maxSplitCount
                    return ("maxSp=" + (if ((maxSplit == 99)) "Any" else (maxSplit).toString()))
                }
            }
            sPrefs.add(splitMaximum)

            val allowAceResplit = object : SwitchPref("Allow re-split for Ace?",
                    "player can re-split Ace split hand",
                    arrayOf("NO", "YES")) {

                override val genuineCheckedValue: Boolean
                    get() = Table.gameRule.allowAceResplit

                override fun settingRuleOut() {
                    Table.gameRule.allowAceResplit = mIsChecked
                }

                override fun getSubString(isChecked: Boolean): String {
                    return (if (isChecked) "AceResplit" else "No_AceResplit")
                }
            }
            sPrefs.add(allowAceResplit)

            val splitDifferentTen = object : SwitchPref("Allow split different 10 values cards?",
                    "player can split different 10 value cards: 10-Jack-Queen-King",
                    arrayOf("NO", "YES")) {

                override val genuineCheckedValue: Boolean
                    get() = Table.gameRule.allowSplitDifferentTenValues

                override fun settingRuleOut() {
                    Table.gameRule.allowSplitDifferentTenValues = mIsChecked
                }

                override fun getSubString(isChecked: Boolean): String {
                    return (if (isChecked) "SpD10" else "No_SpD10")
                }
            }
            sPrefs.add(splitDifferentTen)

            return sPrefs
        }

    val tablePrefCategory: BjPrefCategory
        get() {
            val tPrefs = BjPrefCategory("Table Options")

            // Number of Decks of Shoe: Deck=1 ~ Deck=8
            val shoeDeckCount = object : SpinnerPref("Number of Decks in Shoe",
                    "How many decks are used in Shoe",
                    arrayOf("Single Deck", "Double Deck", "4 Decks", "6 Decks", "8 Decks")) {

                override val genuineIndexValue: Int = when (Table.tableRule.numDecks) {
                    1 -> 0
                    2 -> 1
                    4 -> 2
                    6 -> 3
                    8 -> 4
                    else -> 3
                }

                override fun settingRuleOut() {
                    Table.tableRule.numDecks = getNumDecksByIndex(mSelectedPosition)
                }

                fun getNumDecksByIndex(index: Int): Int = when (index) {
                    0 -> 1
                    1 -> 2
                    2 -> 4
                    3 -> 6
                    4 -> 8
                    else -> 6
                }

                override fun toString(): String {
                    val nDecks = if (mTouched)
                        getNumDecksByIndex(mSelectedPosition)
                    else
                        Table.tableRule.numDecks
                    return "Deck=" + (nDecks).toString()
                }
            }
            tPrefs.add(shoeDeckCount)

            // Max Number of Multi-Hand: Box-1 ~ Box-8
            val tableBoxCount = object : SpinnerPref("Maximum number of multi-hand",
                    "How many boxes for multi player hand",
                    arrayOf("1 Box", "2 Boxes", "3 Boxes", "4 Boxes", "5 Boxes", "6 Boxes", "7 Boxes", "8 Boxes", "20 Boxes")) {

                override val genuineIndexValue: Int = when (Table.tableRule.numBoxes) {
                    1, 2, 3, 4, 5, 6, 7, 8 -> Table.tableRule.numBoxes - 1
                    else -> 8
                }

                override fun settingRuleOut() {
                    Table.tableRule.numBoxes = getNumBoxesByIndex(mSelectedPosition)
                }

                fun getNumBoxesByIndex(index: Int): Int = when (index) {
                    0, 1, 2, 3, 4, 5, 6, 7 -> index + 1
                    else -> 20
                }

                override fun toString(): String {
                    val numBoxes: Int = if (mTouched)
                        getNumBoxesByIndex(mSelectedPosition)
                    else
                        Table.tableRule.numBoxes
                    return "Boxes=" + (numBoxes).toString()
                }
            }
            tPrefs.add(tableBoxCount)

            val tableBlackjackPayout = object : SwitchPref("Blackjack Payout",
                    "Casino pays player's Blackjack with payout ratio: 6 to 5, or 3 to 2",
                    arrayOf("6-5", "3-2")) {
                override val genuineCheckedValue: Boolean
                    get() = (Table.tableRule.blackjackPayout == 1.5f)

                override fun settingRuleOut() {
                    Table.tableRule.blackjackPayout = if (mIsChecked) 1.5f else 1.2f
                }

                override fun getSubString(isChecked: Boolean): String {
                    return ("BJ_Payout=" + (if (isChecked) "3-2" else "6-5"))
                }
            }
            tPrefs.add(tableBlackjackPayout)

            val tableMinBet = object : SpinnerPref("Minimum Betting",
                    "minimum betting of players for each hand",
                    arrayOf("$1", "$5", "$10", "$25")) {
                override val genuineIndexValue: Int = when (Table.tableRule.minBet) {
                    1.0f -> 0
                    5.0f -> 1
                    10.0f -> 2
                    else -> 3
                }

                override fun settingRuleOut() {
                    Table.tableRule.minBet = getMinBetByIndex(mSelectedPosition)
                }

                fun getMinBetByIndex(index: Int): Float = when (index) {
                    0 -> 1.0f
                    1 -> 5.0f
                    2 -> 10.0f
                    3 -> 25.0f
                    else -> 1.0f
                }

                override fun toString(): String {
                    val minBet = if (mTouched)
                        getMinBetByIndex(mSelectedPosition)
                    else
                        Table.tableRule.minBet
                    return "MinBet=" + minBet.toDollarAmountFloor()
                }
            }
            tPrefs.add(tableMinBet)

            val tableMaxBet = object : SpinnerPref("Maximum Bet Amount",
                    "maximum betting of players for each hand",
                    arrayOf("$100", "$1,000", "$10,000", "No Limit")) {
                override val genuineIndexValue: Int = when (Table.tableRule.maxBet) {
                    100.0f -> 0
                    1_000.0f -> 1
                    10_000.0f -> 2
                    else -> 3
                }

                override fun settingRuleOut() {
                    Table.tableRule.maxBet = getMaxBetByIndex(mSelectedPosition)
                }

                fun getMaxBetByIndex(index: Int): Float = when (index) {
                    0 -> 100.0f
                    1 -> 1_000.0f
                    2 -> 10_000.0f
                    3 -> java.lang.Float.MAX_VALUE
                    else -> 1_000.0f
                }

                override fun toString(): String {
                    val maxBet: Float = if (mTouched)
                        getMaxBetByIndex(mSelectedPosition)
                    else
                        Table.tableRule.maxBet

                    return ("MaxBet=" + (if ((maxBet < java.lang.Float.MAX_VALUE))
                        maxBet.toDollarAmountFloor()
                    else
                        "No-Limit"))
                }
            }
            tPrefs.add(tableMaxBet)

            return tPrefs
        }

    val roundPrefCategory: BjPrefCategory
        get() {
            val rCat = BjPrefCategory("Other Options")

            val tablePlayerBankroll = object : SpinnerPref("Player's initial bankroll",
                    "Player starts with bankroll",
                    arrayOf("$100", "$1,000", "$10,000", "$100,000", "$1,000,000", "No-Limit")) {

                override val genuineIndexValue: Int = when (Table.tableRule.initBalance) {
                    100.0f -> 0
                    1_000.0f -> 1
                    10_000.0f -> 2
                    100_000.0f -> 3
                    1_000_000.0f -> 4
                    else -> 5
                }

                override fun settingRuleOut() {
                    Table.tableRule.initBalance = getBankrollByIndex(mSelectedPosition)
                }

                fun getBankrollByIndex(index: Int): Float = when (index) {
                    0 -> 100.0f
                    1 -> 1_000.0f
                    2 -> 10_000.0f
                    3 -> 100_000.0f
                    4 -> 1_000_000.0f
                    5 -> java.lang.Float.MAX_VALUE
                    else -> 10_000.0f
                }

                override fun toString(): String {
                    val bankroll: Float = if (mTouched)
                        getBankrollByIndex(mSelectedPosition)
                    else
                        Table.tableRule.initBalance

                    return ("Bankroll=" + (if ((bankroll < java.lang.Double.MAX_VALUE))
                        bankroll.toDollarAmountFloor()
                    else
                        "No-Limit"))
                }
            }
            rCat.add(tablePlayerBankroll)

            val useRandomShoe = object : SwitchPref("Shoe Random Seed",
                    "shoe uses fixed random seed",
                    arrayOf("Random", "Fixed")) {

                override val genuineCheckedValue: Boolean
                    get() = Table.tableRule.fixedRandom

                override fun settingRuleOut() {
                    Table.tableRule.fixedRandom = mIsChecked
                }

                override fun getSubString(isChecked: Boolean): String {
                    return ("Shoe=" + (if (isChecked) "Random" else "Fixed"))
                }
            }
            rCat.add(useRandomShoe)

            val useSound = object : SwitchPref("Play Sounds",
                    "Game sound can be turned ON or OFF",
                    arrayOf("OFF", "ON")) {

                override val genuineCheckedValue: Boolean
                    get() = Table.tableRule.useSound

                override fun settingRuleOut() {
                    Table.tableRule.useSound = mIsChecked
                }

                override fun getSubString(isChecked: Boolean): String {
                    return ("Sound=" + (if (isChecked) "ON" else "OFF"))
                }
            }
            rCat.add(useSound)

            val useAnimation = object : SwitchPref("Animations",
                    "Card Animations can be turned ON or OFF",
                    arrayOf("OFF", "ON")) {

                override val genuineCheckedValue: Boolean
                    get() = Table.tableRule.useAnimation

                override fun settingRuleOut() {
                    Table.tableRule.useAnimation = mIsChecked
                }

                override fun getSubString(isChecked: Boolean): String {
                    return ("Animation=" + (if (isChecked) "ON" else "OFF"))
                }
            }
            rCat.add(useAnimation)

            return rCat
        }
}

class PreferenceFrag : BjFragment() {
    private var mListener: CountingFrag.OnFragmentInteractionListener? = null

    lateinit var prefFactory: PrefFactory
    var totalPreference = ArrayList<PrefFactory.BjPrefCategory>()

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
                val alert_confirm = AlertDialog.Builder(gMainActivity)
                alert_confirm.setMessage("Save changed settings?").setCancelable(false)
                        .setPositiveButton("Yes") { dialog, which ->
                            updatePreferences()
                            gMainActivity.onBackPressed()
                        }.setNegativeButton("Cancel",
                                DialogInterface.OnClickListener { dialog, which ->
                                    return@OnClickListener
                                })
                val alert = alert_confirm.create()
                alert.show()
            } else {
                gMainActivity.onBackPressed()
            }
            R.id.btnSettingCancel -> if (needUpdate) {
                val alert_confirm = AlertDialog.Builder(gMainActivity)
                alert_confirm.setMessage("Discard changed settings?").setCancelable(false)
                        .setPositiveButton("Yes") { dialog, which
                            ->
                            gMainActivity.onBackPressed()
                        }
                        .setNegativeButton("Cancel",
                                DialogInterface.OnClickListener { dialog, which ->
                                    return@OnClickListener
                                })
                val alert = alert_confirm.create()
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
        var rString = ""
        for (cat in totalPreference) {
            rString += cat.ruleString
        }

        fun spanRuleString(): CharSequence {
            val span = SpannableString(rString)

            var pos = rString.indexOf('[', 0)
            while (pos >= 0) {
                span.setSpan(ForegroundColorSpan(Color.BLUE), pos, pos + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                pos = rString.indexOf('[', pos + 1)
            }

            pos = rString.indexOf(']', 0)
            while (pos >= 0) {
                span.setSpan(ForegroundColorSpan(Color.BLUE), pos, pos + 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                pos = rString.indexOf(']', pos + 1)
            }
            return span
        }

        tvRuleText_Preference.text = spanRuleString()
    }

    //=== adding settings ===
    private fun updatePreferences() {
        for (cat in totalPreference) {
            for (pref in cat) {
                if (pref.needUpdate()) pref.settingRuleOut()
                pref.mTouched = false
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
        fun onListFragmentInteraction(item: PrefFactory.BjPref)
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

    inner class BjPrefCategoryAdapter(private val catList: List<PrefFactory.BjPrefCategory>,
                                      private val mListener: PreferenceFrag.OnListFragmentInteractionListener?)
        : RecyclerView.Adapter<BjPrefCategoryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.row_pref_category, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val cat = catList[position]

//            holder.mView.prefRecycleView.layoutManager = LinearLayoutManager(gMainActivity)
            holder.mView.prefRecycleView.adapter = BjPrefViewAdapter(cat, null)

            holder.mView.tvPrefRowCategoryTitle.typeface = FontUtility.contentFace
            holder.mView.tvPrefRowCategoryTitle.text = cat.categoryName

            holder.mView.tvPrefRowCategoryTitle.setOnClickListener({ v ->
                if (cat.isFolded) {
                    cat.isFolded = false
                    holder.mView.prefRecycleView.visibility = View.VISIBLE
                } else {
                    cat.isFolded = true
                    holder.mView.prefRecycleView.visibility = View.GONE
                }
            })
        }

        override fun getItemCount(): Int {
            return catList.size
        }

        inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
            init {
            }
        }
    }

    inner class BjPrefViewAdapter(private val prefList: PrefFactory.BjPrefCategory,
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
            is PrefFactory.SwitchPref -> PreferenceFrag.TYPE_SWITCH
            is PrefFactory.SpinnerPref -> PreferenceFrag.TYPE_SPINNER
            is PrefFactory.SpinnerTwoPref -> PreferenceFrag.TYPE_SPINNER_TWO
            else -> PreferenceFrag.TYPE_TEXT
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pref = prefList[position]

            holder.mView.tvPrefRowDescription.typeface = FontUtility.contentFace

            when (pref) {
                is PrefFactory.TextPref -> {
                    pref.editText = holder.itemView.textPrefRow

                    pref.editText.setText(pref.mNewName)

                    pref.editText.setOnEditorActionListener({ v, actionId, event ->
                        pref.mNewName = pref.editText.text.toString()
                        if (pref.needUpdate()) {
                            holder.itemView.tvPrefRowTitle.setTextColor(Color.BLUE)  // TODO: Colors
                        } else {
                            holder.itemView.tvPrefRowTitle.setTextColor(Color.BLACK)    // TODO: Colors
                        }
                        updatePreferencesText()
                        true
                    })
                }
                is PrefFactory.SwitchPref -> {
                    pref.switch = holder.itemView.switchPrefRow

                    pref.switch.textOff = pref.dataText[0]    // TODO: check
                    pref.switch.textOn = pref.dataText[1]     // TODO: check

                    pref.switch.setOnCheckedChangeListener({ _, isChecked ->
                        pref.mIsChecked = isChecked
                        if (pref.needUpdate()) {
                            holder.itemView.tvPrefRowTitle.setTextColor(Color.BLUE)  // TODO: Colors
                        } else {
                            holder.itemView.tvPrefRowTitle.setTextColor(Color.BLACK)    // TODO: Colors
                        }
                        updatePreferencesText()
                        pref.postSwitchCheckedChange()
                    })
                }
                is PrefFactory.SpinnerPref -> {
                    pref.spinner = holder.itemView.spinnerPrefRow

                    val dataAdapter = BjArrayAdapter(gMainActivity,
                            R.layout.simple_spinner_dropdown_item,
                            pref.dataText)
                    dataAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                    pref.spinner.adapter = dataAdapter

                    pref.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                            pref.mSelectedPosition = position
                            if (pref.needUpdate()) {
                                holder.itemView.tvPrefRowTitle.setTextColor(Color.BLUE)
                            } else {
                                holder.itemView.tvPrefRowTitle.setTextColor(Color.BLACK)
                            }
                            updatePreferencesText()
                            pref.postSpinnerItemSelected()
                        }

                        override fun onNothingSelected(parent: AdapterView<*>?) {
                        }
                    }

                    if (pref is PrefFactory.SpinnerTwoPref) {
                        val dataAdapterTwo = BjArrayAdapter(gMainActivity,
                                R.layout.simple_spinner_dropdown_item,
                                pref.mDataStringsTwo)

                        dataAdapterTwo.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
                        pref.spinnerTwo.adapter = dataAdapterTwo

                        pref.spinnerTwo.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                                pref.mSelectedPositionTwo = position
                                if (pref.needUpdate()) {
                                    holder.itemView.tvPrefRowTitle.setTextColor(Color.BLUE)
                                } else {
                                    holder.itemView.tvPrefRowTitle.setTextColor(Color.BLACK)
                                }
                                updatePreferencesText()
                                pref.postSpinnerTwoItemSelected()
                            }

                            override fun onNothingSelected(parent: AdapterView<*>) {}
                        }
                    }
                }
                else -> {
                }
            }

            holder.mView.setOnClickListener {
                mListener?.onListFragmentInteraction(pref)
            }

            holder.mView.tvPrefRowTitle.text = pref.title
            holder.mView.tvPrefRowDescription.text = pref.description

            pref.settingRuleIn()
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
