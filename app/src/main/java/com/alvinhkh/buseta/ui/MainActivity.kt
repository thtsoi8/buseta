package com.alvinhkh.buseta.ui

import android.app.ActivityManager
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat

import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.follow.dao.FollowDatabase
import com.alvinhkh.buseta.follow.ui.FollowGroupFragment
import com.alvinhkh.buseta.mtr.ui.MtrLineStatusFragment
import com.alvinhkh.buseta.search.ui.HistoryFragment
import com.alvinhkh.buseta.service.ProviderUpdateService
import com.alvinhkh.buseta.utils.AdViewUtil
import com.alvinhkh.buseta.utils.ColorUtil


class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_follow)

        setToolbar()
        val actionBar = supportActionBar
        actionBar?.run {
            setTitle(R.string.app_name)
            subtitle = null
            setDisplayHomeAsUpEnabled(false)
        }

        val followGroupFragment = FollowGroupFragment()
        val historyFragment = HistoryFragment()
        val mtrLineStatusFragment = MtrLineStatusFragment()

        adViewContainer = findViewById(R.id.adView_container)
        if (adViewContainer != null) {
            adView = AdViewUtil.banner(adViewContainer, adView, false)
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            var colorInt = 0
            var title = getString(R.string.app_name)
            val fm = supportFragmentManager ?: return@setOnNavigationItemSelectedListener false
            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            when (item.itemId) {
                R.id.action_follow -> {
                    if (fm.findFragmentByTag("follow_list") == null) {
                        title = getString(R.string.app_name)
                        colorInt = ContextCompat.getColor(this, R.color.colorPrimary)
                        val ft = fm.beginTransaction()
                        ft.replace(R.id.fragment_container, followGroupFragment)
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        ft.addToBackStack("follow_list")
                        ft.commit()
                    }
                }
                R.id.action_search_history -> {
                    if (fm.findFragmentByTag("search_history") == null) {
                        title = getString(R.string.app_name)
                        colorInt = ContextCompat.getColor(this, R.color.colorPrimary)
                        val ft = fm.beginTransaction()
                        ft.replace(R.id.fragment_container, historyFragment)
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        ft.addToBackStack("search_history")
                        ft.commit()
                    }
                }
                R.id.action_railway -> {
                    if (fm.findFragmentByTag("railway") == null) {
                        title = getString(R.string.provider_mtr)
                        colorInt = ContextCompat.getColor(this, R.color.provider_mtr)
                        val ft = fm.beginTransaction()
                        ft.replace(R.id.fragment_container, mtrLineStatusFragment)
                        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                        ft.addToBackStack("railway")
                        ft.commit()
                    }
                }
                else -> finish()
            }
            supportActionBar?.title = title
            supportActionBar?.subtitle = null
            if (colorInt != 0) {
                supportActionBar?.setBackgroundDrawable(ColorDrawable(colorInt))
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && window != null) {
                    window.statusBarColor = ColorUtil.darkenColor(colorInt)
                    window.navigationBarColor = ColorUtil.darkenColor(colorInt)
                }
            }
            if (Build.VERSION.SDK_INT >= 28) {
                setTaskDescription(ActivityManager.TaskDescription(title, R.mipmap.ic_launcher,
                        ContextCompat.getColor(this, R.color.colorPrimary600)))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                @Suppress("DEPRECATION")
                setTaskDescription(ActivityManager.TaskDescription(title, bm,
                        ContextCompat.getColor(this, R.color.colorPrimary600)))
            }
            true
        }
        supportFragmentManager.addOnBackStackChangedListener {
            val f = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (f != null && bottomNavigationView != null) {
                when (f.javaClass.name) {
                    "FollowFragment" -> bottomNavigationView.selectedItemId = R.id.action_follow
                    "HistoryFragment" -> bottomNavigationView.selectedItemId = R.id.action_search_history
                    "MtrLineStatusFragment" -> bottomNavigationView.selectedItemId = R.id.action_railway
                }
            }

        }
        if (bottomNavigationView != null) {
            val followDatabase = FollowDatabase.getInstance(applicationContext)
            if (followDatabase != null && followDatabase.followDao().count() > 0) {
                bottomNavigationView.selectedItemId = R.id.action_follow
            } else {
                bottomNavigationView.selectedItemId = R.id.action_search_history
            }
        }

        try {
            startService(Intent(this, ProviderUpdateService::class.java))
        } catch (ignored: Throwable) {
        }

    }

    override fun onBackPressed() {
        when {
            supportFragmentManager.backStackEntryCount < 2 -> finish()
            supportFragmentManager.backStackEntryCount > 0 -> supportFragmentManager.popBackStack()
            else -> super.onBackPressed()
        }
    }
}
