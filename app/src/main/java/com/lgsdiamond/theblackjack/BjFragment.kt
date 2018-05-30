package com.lgsdiamond.theblackjack

import android.app.Fragment
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.view.View


/**
 * Created by LgsDi on 2018-03-10.
 */
abstract class BjFragment : Fragment() {
    abstract var barTitle: String

    fun setActionBarTitle() {
        val span = SpannableString(barTitle)
        span.setSpan(CustomTypefaceSpan("", FontUtility.titleFace),
                0, barTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        gMainActivity.supportActionBar?.title = span
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initFragmentUI(view)
    }

    abstract fun initFragmentUI(view: View)
}