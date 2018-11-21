package com.alvinhkh.buseta.route.ui

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.alvinhkh.buseta.model.Route

class RoutePagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

    private var fragmentList: MutableList<Fragment> = ArrayList()

    internal var routeList: MutableList<Route> = ArrayList()

    private var pageTitleList: MutableList<String> = ArrayList()

    override fun getItem(position: Int) = fragmentList[position]

    override fun getCount() = fragmentList.size

    override fun getPageTitle(position: Int): CharSequence? {
        return pageTitleList[position]
    }

    fun addFragment(fragment: Fragment, pageTitle: String, route: Route) {
        fragmentList.add(fragment)
        pageTitleList.add(pageTitle)
        routeList.add(route)
        notifyDataSetChanged()
    }

    fun clear() {
        fragmentList.clear()
        pageTitleList.clear()
        routeList.clear()
        notifyDataSetChanged()
    }
}