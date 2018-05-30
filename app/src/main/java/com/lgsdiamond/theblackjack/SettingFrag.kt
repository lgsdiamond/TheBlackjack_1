package com.lgsdiamond.theblackjack

import android.app.Fragment
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceFragment
import android.support.v4.content.ContextCompat
import android.text.Spannable
import android.text.SpannableString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SettingFrag.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SettingFrag.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingFrag : PreferenceFragment() {
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

        addPreferencesFromResource(R.xml.preferences)
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.setBackgroundColor(ContextCompat.getColor(gMainActivity, R.color.background_material_light))
        return view
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the sActivity and potentially other fragments contained in that
     * sActivity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    lateinit var barTitle: String

    fun setActionBarTitle() {
        val span = SpannableString(barTitle)
        span.setSpan(CustomTypefaceSpan("", FontUtility.titleFace),
                0, barTitle.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        gMainActivity.supportActionBar?.title = span
    }

    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param BAR_TITLE Parameter 1.
         * @return A new instance of fragment BettingFrag.
         */

        private const val BAR_TITLE = "BAR_TITLE"

        fun newInstance(barTitle: String): SettingFrag {
            val fragment = SettingFrag()
            val args = Bundle()
            args.putString(BAR_TITLE, barTitle)
            fragment.arguments = args
            fragment.barTitle = barTitle

            return fragment
        }
    }
}
