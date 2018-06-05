package com.lgsdiamond.theblackjack

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.util.DisplayMetrics
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

const val PACKAGE_NAME = "com.lgsdiamond.theblackjack"
const val PREF_NAME = "TheBlackjackPref"

lateinit var gMainActivity: MainActivity
val gContext: Context
    get() = gMainActivity.applicationContext

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        BettingFrag.OnFragmentInteractionListener,
        CountingFrag.OnFragmentInteractionListener,
        LearningFrag.OnFragmentInteractionListener,
        GameFrag.OnFragmentInteractionListener,
        PreferenceFrag.OnListFragmentInteractionListener,
        SimulationFrag.OnFragmentInteractionListener,
        StatisticsFrag.OnFragmentInteractionListener,
        StrategyFrag.OnFragmentInteractionListener {

    val defPreferences: SharedPreferences by lazy { getSharedPreferences(PREF_NAME, 0) }

    private var currentFrag: BjFragment? = null

    private val bettingFrag = BettingFrag.newInstance("Betting Strategy")
    private val countingFrag = CountingFrag.newInstance("Card Counting")
    private val learningFrag = LearningFrag.newInstance("Learning Blackjack")
    private val gameFrag = GameFrag.newInstance("Blackjack Table")
    private val preferenceFrag = PreferenceFrag.newInstance("Preferences")
    private val simulationFrag = SimulationFrag.newInstance("Simulation")
    private val statisticsFrag = StatisticsFrag.newInstance("Statistics")
    private val strategyFrag = StrategyFrag.newInstance("Strategy")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // fix orientation to vertical
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // activate utility functions in the very beginning of main sActivity
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        makeActionBarTitleSpan()

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        FontUtility.customFaceMenu(nav_view.menu, FontUtility.titleFace)

        initMainUI()

        initFragments()
    }

    private fun makeActionBarTitleSpan() {
        val title = "TheBlackjack"
        val span = SpannableString(title)
        span.setSpan(CustomTypefaceSpan("", FontUtility.titleFace),
                0, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(RelativeSizeSpan(0.8f),
                0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(ForegroundColorSpan(Color.YELLOW),
                0, 3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(ForegroundColorSpan(Color.GREEN),
                3, title.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

        supportActionBar?.title = span
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            val count = fragmentManager.backStackEntryCount
            if (count > 0) {
                fragmentManager.popBackStack()
            } else {
                finishApp(true)
            }
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        "onResumeFragments".toToastShort()
    }

    override fun onPause() {
        super.onPause()
        "onPause".toToastShort()
    }

    private fun finishApp(toAsk: Boolean) {
        if (toAsk) {
            val builder = AlertDialog.Builder(this)
                    .setTitle("Exit TheBlackjack".spanTitleFace())
                    .setMessage("Do you want to exit TheBlackjack?")
                    .setPositiveButton("Yes") { _, _ ->
                        moveTaskToBack(true)
                        finish()
                        android.os.Process.killProcess(android.os.Process.myPid())
                    }
                    .setNegativeButton("No", null)

            val dialog = builder.create()
            dialog.show()
        } else {
            moveTaskToBack(true)
            finish()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        setIconInMenu(menu, R.id.action_settings, R.string.action_settings, R.drawable.ic_setting)
        setIconInMenu(menu, R.id.action_about, R.string.action_about, R.drawable.ic_about)
        setIconInMenu(menu, R.id.action_finish, R.string.action_finish, R.drawable.ic_finish)

        FontUtility.customFaceMenu(menu, FontUtility.titleFace)

        return true
    }

    private fun setIconInMenu(menu: Menu, menuItemId: Int, labelId: Int, iconId: Int) {
        val item = menu.findItem(menuItemId)
        val builder = SpannableStringBuilder("  " + resources.getString(labelId))
        builder.setSpan(ImageSpan(this, iconId), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        item.title = builder
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent sActivity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_settings -> {
                switchFragment(preferenceFrag)
                return true
            }
            R.id.action_about -> {
                runSettingActivity()
//                showAbout() // TODO-
                return true
            }
            R.id.action_finish -> {
                finishApp(false)
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onFragmentInteraction(uri: Uri) {
        // empty body
    }

    override fun onListFragmentInteraction(item: PreferenceFrag.BjPref) {
        // empty body
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_play -> {
                switchFragment(gameFrag)
            }
            R.id.nav_simulator -> {
                switchFragment(simulationFrag)
            }
            R.id.nav_counting -> {
                switchFragment(countingFrag)
            }
            R.id.nav_strategy -> {
                switchFragment(strategyFrag)
            }
            R.id.nav_betting -> {
                switchFragment(bettingFrag)
            }
            R.id.nav_learning -> {
                switchFragment(learningFrag)
            }
            R.id.nav_statistics -> {
                switchFragment(statisticsFrag)
            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    //===
    private fun initMainUI() {
        val winManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        winManager.defaultDisplay.getMetrics(sMetrics)

        btnTest.setOnClickListener({ _: View ->
            runTest()
        })

        btnStart.setOnClickListener({ _: View ->
            startGame()
        })
    }

    private fun initFragments() {
    }

    private fun switchFragment(frag: BjFragment) {
        if (frag == currentFrag) return

        val transaction = fragmentManager.beginTransaction()

        // add backStack for valid frag
        if ((currentFrag != null) && (currentFrag != preferenceFrag))
            transaction.addToBackStack(currentFrag?.barTitle)

        transaction.replace(R.id.loFragHolder, frag, frag.barTitle)
        transaction.show(frag)
        transaction.commit()

        frag.setActionBarTitle()
        currentFrag = frag        // now it becomes current
    }

    private fun showAbout() {

    }

    private fun runSettingActivity() {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent)
    }

    //== TESTING ==
    private fun runTest() {
    }

    private fun startGame() {
        switchFragment(gameFrag)
    }

    companion object {
        var sMetrics: DisplayMetrics = DisplayMetrics()
    }

    init {
        gMainActivity = this
    }
}