package com.lgsdiamond.theblackjack

import android.app.Fragment
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [StatisticsFrag.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [StatisticsFrag.newInstance] factory method to
 * create an instance of this fragment.
 */
class StatisticsFrag : BjFragment() {
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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun initFragmentUI(view: View) {

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

        fun newInstance(barTitle: String): StatisticsFrag {
            val fragment = StatisticsFrag()
            val args = Bundle()
            args.putString(BAR_TITLE, barTitle)
            fragment.arguments = args
            fragment.barTitle = barTitle

            return fragment
        }
    }
}
