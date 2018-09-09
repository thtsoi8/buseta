package com.alvinhkh.buseta.model

import android.os.Parcel
import android.os.Parcelable
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.follow.model.Follow

data class RouteStop(
        var stopId: String? = null,
        var companyCode: String? = null,
        var routeDestination: String? = null,
        var routeSeq: String? = null,
        var etaGet: String? = null,
        var fare: String? = null,
        var fareHoliday: String? = null,
        var fareChild: String? = null,
        var fareSenior: String? = null,
        var imageUrl: String? = null,
        var latitude: String? = null,
        var location: String? = null,
        var longitude: String? = null,
        var name: String? = null,
        var routeOrigin: String? = null,
        var sequence: String? = null,
        var routeNo: String? = null,
        var routeId: String? = null,
        var routeServiceType: String? = null,
        var description: String? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(stopId)
        parcel.writeString(companyCode)
        parcel.writeString(routeDestination)
        parcel.writeString(routeSeq)
        parcel.writeString(etaGet)
        parcel.writeString(fare)
        parcel.writeString(fareHoliday)
        parcel.writeString(fareChild)
        parcel.writeString(fareSenior)
        parcel.writeString(imageUrl)
        parcel.writeString(latitude)
        parcel.writeString(location)
        parcel.writeString(longitude)
        parcel.writeString(name)
        parcel.writeString(routeOrigin)
        parcel.writeString(sequence)
        parcel.writeString(routeNo)
        parcel.writeString(routeId)
        parcel.writeString(routeServiceType)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<RouteStop> {
        override fun createFromParcel(parcel: Parcel): RouteStop {
            return RouteStop(parcel)
        }

        override fun newArray(size: Int): Array<RouteStop?> {
            return arrayOfNulls(size)
        }

        fun toFollow(routeStop: RouteStop): Follow {
            val follow = Follow()
            follow.type = Follow.TYPE_ROUTE_STOP
            follow.companyCode = routeStop.companyCode?:""
            if (follow.companyCode == C.PROVIDER.MTR) {
                follow.type = Follow.TYPE_RAILWAY_STOP
            }
            follow.routeId = routeStop.routeId?:""
            follow.routeNo = routeStop.routeNo?:""
            follow.routeSeq = routeStop.routeSeq?:""
            follow.routeServiceType = routeStop.routeServiceType?:""
            follow.routeDestination = routeStop.routeDestination?:""
            follow.routeOrigin = routeStop.routeOrigin?:""
            follow.stopId = routeStop.stopId?:""
            follow.stopSeq = routeStop.sequence?:""
            follow.stopName = routeStop.name?:""
            follow.stopLatitude = routeStop.latitude?:""
            follow.stopLongitude = routeStop.longitude?:""
            follow.etaGet = routeStop.etaGet?:""
            return follow
        }
    }
}