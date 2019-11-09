package com.alvinhkh.buseta.nwst

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.*
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.arrivaltime.dao.ArrivalTimeDatabase
import com.alvinhkh.buseta.arrivaltime.model.ArrivalTime
import com.alvinhkh.buseta.nwst.model.NwstEta
import com.alvinhkh.buseta.nwst.util.NwstRequestUtil
import com.alvinhkh.buseta.route.dao.RouteDatabase
import com.alvinhkh.buseta.utils.HashUtil
import org.jsoup.Jsoup
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern

class NwstEtaWorker(private val context : Context, params : WorkerParameters)
    : Worker(context, params) {

    private val nwstService = NwstService.api.create(NwstService::class.java)

    private val arrivalTimeDatabase = ArrivalTimeDatabase.getInstance(context)!!

    private val routeDatabase = RouteDatabase.getInstance(context)!!

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    override fun doWork(): Result {
        val widgetId = inputData.getInt(C.EXTRA.WIDGET_UPDATE, -1)
        val notificationId = inputData.getInt(C.EXTRA.NOTIFICATION_ID, -1)
        val companyCode = inputData.getString(C.EXTRA.COMPANY_CODE)?:C.PROVIDER.NWST
        val routeId = inputData.getString(C.EXTRA.ROUTE_ID)?:return Result.failure()
        val routeNo = inputData.getString(C.EXTRA.ROUTE_NO)?:return Result.failure()
        val routeSequence = inputData.getString(C.EXTRA.ROUTE_SEQUENCE)?:return Result.failure()
        val routeServiceType = inputData.getString(C.EXTRA.ROUTE_SERVICE_TYPE)?:return Result.failure()
        val stopId = inputData.getString(C.EXTRA.STOP_ID)?:return Result.failure()
        val stopSequence = inputData.getString(C.EXTRA.STOP_SEQUENCE)?:return Result.failure()

        val outputData = Data.Builder()
                .putInt(C.EXTRA.WIDGET_UPDATE, widgetId)
                .putInt(C.EXTRA.NOTIFICATION_ID, notificationId)
                .putString(C.EXTRA.COMPANY_CODE, companyCode)
                .putString(C.EXTRA.ROUTE_ID, routeNo)
                .putString(C.EXTRA.ROUTE_NO, routeNo)
                .putString(C.EXTRA.ROUTE_SEQUENCE, routeSequence)
                .putString(C.EXTRA.ROUTE_SERVICE_TYPE, routeServiceType)
                .putString(C.EXTRA.STOP_ID, stopId)
                .putString(C.EXTRA.STOP_SEQUENCE, stopSequence)
                .build()

        val routeStop = routeDatabase.routeStopDao().get(companyCode, routeNo, routeSequence, routeServiceType, stopId, stopSequence)
                ?: return Result.failure(outputData)
        
        try {
            var response = nwstService.eta((routeStop.stopId?:"0").toInt().toString(),
                    routeStop.routeNo?:"", "Y", "60", NwstService.LANGUAGE_TC, routeStop.routeSequence?:"",
                    routeStop.sequence?:"", routeStop.routeId?:"", "Y", "Y",
                    NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2).execute()
            if (!response.isSuccessful) {
                var tk = preferences.getString("nwst_tk", "")?:""
                val r0 = nwstService.pushTokenEnable(tk, tk, NwstService.LANGUAGE_TC, "", "Y", NwstService.DEVICETYPE,
                        NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
                        NwstRequestUtil.syscode2()).execute()
                val resText = r0.body()?.string()?:""
                if (resText != "Already Registered") {
                    tk = HashUtil.randomHexString(64)
                    val r1 = nwstService.pushToken(tk, tk, NwstService.LANGUAGE_TC, "", "R", NwstService.DEVICETYPE,
                            NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
                            NwstRequestUtil.syscode2()).execute()
                    val r2 = nwstService.pushTokenEnable(tk, tk, NwstService.LANGUAGE_TC, "", "Y", NwstService.DEVICETYPE,
                            NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
                            NwstRequestUtil.syscode2()).execute()
                    val r3 = nwstService.adv(NwstService.LANGUAGE_TC, "640",
                            NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2,
                            NwstRequestUtil.syscode2(), tk).execute()
                    val editor = preferences.edit()
                    editor.putString("nwst_tk", tk)
                    editor.apply()
                }

                response = nwstService.eta((routeStop.stopId?:"0").toInt().toString(),
                        routeStop.routeNo?:"", "Y", "60", NwstService.LANGUAGE_TC, routeStop.routeSequence?:"",
                        routeStop.sequence?:"", routeStop.routeId?:"", "Y", "Y",
                        NwstRequestUtil.syscode(), NwstService.PLATFORM, NwstService.APP_VERSION, NwstService.APP_VERSION2, NwstRequestUtil.syscode2(), tk).execute()
            }
            if (!response.isSuccessful) {
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTime.text = context.getString(R.string.message_fail_to_request)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                return Result.failure(outputData)
            }

            val arrivalTimeList = arrayListOf<ArrivalTime>()
            val timeNow = System.currentTimeMillis()

            val res = response.body()
            if (res == null || res.contentLength() < 10) {
                val arrivalTime = ArrivalTime.emptyInstance(applicationContext, routeStop)
                arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTime)
                return Result.failure(outputData)
            }

            val text = res.string()
            val serverTime = text.split("\\|".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0].trim { it <= ' ' }
            val data = text.trim { it <= ' ' }.replaceFirst("^[^|]*\\|##\\|".toRegex(), "").split("<br>".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (i in data.indices) {
                val nwstEta = NwstEta.fromString(data[i]) ?: continue
                nwstEta.serverTime = serverTime.replace("[^0-9:]".toRegex(), "")

                val arrivalTime = ArrivalTime.emptyInstance(context, routeStop)
                arrivalTime.companyCode = C.PROVIDER.NWST
                if (nwstEta.companyCode == C.PROVIDER.CTB || nwstEta.companyCode == C.PROVIDER.NWFB) {
                    arrivalTime.companyCode = nwstEta.companyCode
                }
                arrivalTime.text = text(nwstEta.title)
                val subtitle = text(nwstEta.subtitle).trim()
                if (subtitle.isNotEmpty()) {
                    if (subtitle.contains("距離") || subtitle.contains("距离") || subtitle.contains("Distance")) {
                        arrivalTime.distanceKM = parseDistance(subtitle).toDouble()
                    }
                    if (arrivalTime.distanceKM < 0) {
                        arrivalTime.text = arrivalTime.text + " " + subtitle
                    }
                }
                arrivalTime.note = nwstEta.boundText.trim { it <= ' ' }
                arrivalTime.isoTime = nwstEta.etaIsoTime
                arrivalTime.isSchedule = nwstEta.subtitle.isNotEmpty() && (
                        nwstEta.subtitle.contains("預定班次") ||
                                nwstEta.subtitle.contains("预定班次") ||
                                nwstEta.subtitle.contains("Scheduled") ||
                                nwstEta.subtitle.contains("非實時") ||
                                nwstEta.subtitle.contains("非实时") ||
                                nwstEta.subtitle.contains("Non-Real time"))
                val data1 = nwstEta.serverTime.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (data1.size == 3) {
                    try {
                        val c = Calendar.getInstance()
                        c.set(Calendar.HOUR, data1[0].toInt())
                        c.set(Calendar.MINUTE, data1[1].toInt())
                        c.set(Calendar.SECOND, data1[2].toInt())
                        arrivalTime.generatedAt = c.time.time
                    } catch (ignored: NumberFormatException) {
                    }
                }
                arrivalTime.companyCode = routeStop.companyCode?:""
                arrivalTime.routeNo = routeStop.routeNo?:""
                arrivalTime.routeSeq = routeStop.routeSequence?:""
                arrivalTime.stopId = routeStop.stopId?:""
                arrivalTime.stopSeq = routeStop.sequence?:""
                arrivalTime.order = i.toString()

                arrivalTime.updatedAt = timeNow
                arrivalTimeList.add(arrivalTime)
            }
            arrivalTimeDatabase.arrivalTimeDao().insert(arrivalTimeList)
            return Result.success(outputData)
        } catch (e: Exception) {
            Timber.d(e)
        }

        return Result.failure(outputData)
    }

    fun text(text: String): String {
        return Jsoup.parse(text).text().replace("　".toRegex(), " ")
                .replace(" ?預定班次".toRegex(), "")
                .replace(" ?预定班次".toRegex(), "")
                .replace(" ?Scheduled".toRegex(), "")
                .replace(" ?非實時".toRegex(), "")
                .replace(" ?非实时".toRegex(), "")
                .replace(" ?Non-Real time".toRegex(), "")
                .replace("現時未能提供預計抵站時間，請參閱時間表。", "未能提供預計時間")
                .replace("现时未能提供预计抵站时间，请参阅时间表。", "未能提供预计时间")
                .replace("Estimated Time of Arrival is currently not available. Please refer to timetable.", "ETA not available")
                .replace("預計未來([0-9]+)分鐘沒有抵站班次或服務時間已過".toRegex(), "$1分鐘+/已過服務時間")
                .replace("预计未来([0-9]+)分钟没有抵站班次或服务时间已过".toRegex(), "$1分钟+/已过服务时间")
                .replace("No departure estimated in the next ([0-9]+) min or outside service hours".toRegex(), "$1 mins+/outside service hours")
                .replace("。$".toRegex(), "").replace("\\.$".toRegex(), "")
                .replace("往: ", "往")
                .replace(" ?新巴".toRegex(), "")
                .replace(" ?城巴".toRegex(), "")
                .replace("^:\\($".toRegex(), context.getString(R.string.provider_no_eta))
    }

    private fun parseDistance(text: String): Float {
        if (text.isEmpty()) return -1.0f
        val matcher = Pattern.compile("[距離|距离|Distance]: (\\d*\\.?\\d*)").matcher(text)
        if (matcher.find()) {
            var distanceKM = matcher.group(1).toFloat()
            if (distanceKM < 0.0f) {
                distanceKM = -1.0f
            } else if (distanceKM > 10000.0f) {
                // filter out extreme value
                distanceKM = -1.0f
            }
            return distanceKM
        }
        return -1.0f
    }
}