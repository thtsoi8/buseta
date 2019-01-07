package com.alvinhkh.buseta.route.ui

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.route.model.RouteStop

class RouteStopListViewModel(application: Application) : AndroidViewModel(application) {

    private val routeDatabase = RouteDatabase.getInstance(application)

    lateinit var list: LiveData<MutableList<RouteStop>>

    init {
        if (routeDatabase != null) {
            list = routeDatabase.routeStopDao().liveData()
        }
    }

    fun getAsLiveData(): LiveData<MutableList<RouteStop>> {
        return list
    }

    fun getAsLiveData(companyCode: String, routeNo: String, routeSequence: String, routeServiceType: String): LiveData<MutableList<RouteStop>> {
        if (routeDatabase != null) {
            return routeDatabase.routeStopDao().liveData(companyCode, routeNo, routeSequence, routeServiceType)
        }
        return list
    }
}