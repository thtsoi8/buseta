package com.alvinhkh.buseta.service

import android.app.IntentService
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.preference.PreferenceManager
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.alvinhkh.buseta.Api
import com.alvinhkh.buseta.C
import com.alvinhkh.buseta.R
import com.alvinhkh.buseta.datagovhk.LrtFeederWorker
import com.alvinhkh.buseta.datagovhk.MtrLineWorker
import com.alvinhkh.buseta.kmb.KmbEtaRouteWorker
import com.alvinhkh.buseta.mtr.AESBusWorker
import com.alvinhkh.buseta.mtr.MtrResourceWorker
import com.alvinhkh.buseta.nlb.NlbWorker
import com.alvinhkh.buseta.nwst.NwstRouteWorker
import com.alvinhkh.buseta.search.dao.SuggestionDatabase
import com.alvinhkh.buseta.utils.ConnectivityUtil
import timber.log.Timber

class ProviderUpdateService: IntentService(TAG) {

    private lateinit var suggestionDatabase: SuggestionDatabase

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        suggestionDatabase = SuggestionDatabase.getInstance(this)!!
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onHandleIntent(intent: Intent?) {
        val manualUpdate = intent?.getBooleanExtra(C.EXTRA.MANUAL, false)?:false
        if (!ConnectivityUtil.isConnected(this)) {
            // Check internet connection
            val i = Intent(C.ACTION.SUGGESTION_ROUTE_UPDATE)
            i.putExtra(C.EXTRA.UPDATED, true)
            i.putExtra(C.EXTRA.MANUAL, manualUpdate)
            i.putExtra(C.EXTRA.MESSAGE_RID, R.string.message_no_internet_connection)
            sendBroadcast(i)
            return
        }
        WorkManager.getInstance().cancelAllWorkByTag(TAG)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val timeNow = System.currentTimeMillis() / 1000
        val savedTime = sharedPreferences.getLong("last_update_suggestions", timeNow)
        if (!manualUpdate && timeNow < savedTime + 21600) {
            Timber.d("recently updated and not manual update")
            return
        }
        sharedPreferences.edit().putLong("last_update_suggestions", timeNow).apply()

        try {
            val apiService = Api.retrofit.create(Api::class.java)
            val response = apiService.appUpdate().execute()
            if (response.isSuccessful) {
                val res = response.body()
                if (res != null && res.isNotEmpty()) {
                    val appUpdate = res[0]
                    val i = Intent(C.ACTION.APP_UPDATE)
                    i.putExtra(C.EXTRA.UPDATED, true)
                    i.putExtra(C.EXTRA.MANUAL, manualUpdate)
                    i.putExtra(C.EXTRA.APP_UPDATE_OBJECT, appUpdate)
                    sendBroadcast(i)
                }
            }
        } catch (ignored: Throwable) {

        }

        val dataAesBus = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, C.PROVIDER.AESBUS)
                .build()
        WorkManager.getInstance()
                .beginWith(OneTimeWorkRequest.Builder(MtrResourceWorker::class.java).addTag(TAG)
                        .setInputData(dataAesBus).build())
                .then(OneTimeWorkRequest.Builder(AESBusWorker::class.java).addTag(TAG)
                        .setInputData(dataAesBus).build())
                .enqueue()

        val dataKmb = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, C.PROVIDER.KMB)
                .build()
        WorkManager.getInstance()
                .enqueue(OneTimeWorkRequest.Builder(KmbEtaRouteWorker::class.java).addTag(TAG)
                        .setInputData(dataKmb).build())

        val dataLrtFeeder = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, C.PROVIDER.LRTFEEDER)
                .build()
        WorkManager.getInstance()
                .enqueue(OneTimeWorkRequest.Builder(LrtFeederWorker::class.java).addTag(TAG)
                        .setInputData(dataLrtFeeder).build())

        val dataMtrLine = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, C.PROVIDER.MTR)
                .build()
        WorkManager.getInstance()
                .enqueue(OneTimeWorkRequest.Builder(MtrLineWorker::class.java).addTag(TAG)
                        .setInputData(dataMtrLine).build())

        val dataNlb = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, C.PROVIDER.NLB)
                .build()
        WorkManager.getInstance()
                .enqueue(OneTimeWorkRequest.Builder(NlbWorker::class.java).addTag(TAG)
                        .setInputData(dataNlb).build())

        val dataNwst = Data.Builder()
                .putBoolean(C.EXTRA.MANUAL, manualUpdate)
                .putString(C.EXTRA.COMPANY_CODE, C.PROVIDER.NWST)
                .build()
        WorkManager.getInstance()
                .enqueue(OneTimeWorkRequest.Builder(NwstRouteWorker::class.java).addTag(TAG)
                        .setInputData(dataNwst).build())
    }

    companion object {
        private const val TAG = "ProviderUpdateService"
    }

}