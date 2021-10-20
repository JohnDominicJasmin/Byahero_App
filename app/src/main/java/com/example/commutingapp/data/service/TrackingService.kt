package com.example.commutingapp.data.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.commutingapp.R
import com.example.commutingapp.utils.others.Constants
import com.example.commutingapp.utils.others.Constants.ACTION_PAUSE_SERVICE
import com.example.commutingapp.utils.others.Constants.ACTION_START_OR_RESUME_SERVICE
import com.example.commutingapp.utils.others.Constants.ACTION_STOP_SERVICE
import com.example.commutingapp.utils.others.Constants.NOTIFICATION_ID
import com.example.commutingapp.utils.others.TrackingPermissionUtility.hasLocationPermission
import com.example.commutingapp.utils.others.WatchFormatter
import com.example.commutingapp.views.ui.fragments.CommuterFragment
import com.google.android.gms.location.*
import com.mapbox.mapboxsdk.geometry.LatLng
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

typealias innerPolyline = MutableList<LatLng>
typealias outerPolyline = MutableList<innerPolyline>

@AndroidEntryPoint
open class TrackingService : LifecycleService() {

    @Inject lateinit var fusedLocationClient: FusedLocationProviderClient
    @Inject lateinit var baseTrackingNotificationBuilder:NotificationCompat.Builder
    lateinit var currentTrackingNotificationBuilder: NotificationCompat.Builder
    private var isFirstRun = true


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        receiveActionCommand(intent)
        return super.onStartCommand(intent, flags, startId)
    }


    companion object {
        private val trackingPolyLine: TrackingPolyLine = TrackingPolyLine()
        private val liveDataOuterPolyline = trackingPolyLine.polyLine()
        private val stopWatch = TrackingStopWatch()
        val is_Tracking = MutableLiveData<Boolean>() //Todo (reason = refactor later create function for the query)
        val timeInMillis = stopWatch.getTimeRunMillis()

    }

    fun isCurrentlyTracking(): LiveData<Boolean> = is_Tracking
    fun outerPolyline(): MutableLiveData<outerPolyline> = liveDataOuterPolyline

    private fun postInitialValues() {
        is_Tracking.postValue(false)
        liveDataOuterPolyline.postValue(mutableListOf())
        stopWatch.postInitialValues()
    }

    override fun onCreate() {
        super.onCreate()
        currentTrackingNotificationBuilder = baseTrackingNotificationBuilder
        postInitialValues()
        subscribeToObservers()

    }
    private fun subscribeToObservers(){
        is_Tracking.observe(this) {
            createLocationRequest()
            createNotification()
        }

    }
    private fun pauseService() {
        is_Tracking.postValue(false)
        stopWatch.pause()
    }


    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            if (isTracking()) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    trackingPolyLine.addPolyline(location)
                }
            }
        }
    }


    private fun createLocationRequest() {

        if (!isTracking()) {
            removeLocationUpdates()
            return
        }
        requestLocationUpdates()
    }


    private fun receiveActionCommand(intent: Intent?) {
        intent?.let {
            when (it.action) {
                ACTION_START_OR_RESUME_SERVICE -> {
                    if (isFirstRun) {
                        startForegroundService()
                        isFirstRun = false // todo fix startTimer
                        return@let
                    }
                    startTimer()
                    Timber.e("Resumed")
                }
                ACTION_PAUSE_SERVICE -> {
                    pauseService()
                }
                ACTION_STOP_SERVICE -> {
                    Timber.e("Stopped")
                }
            }
        }
    }


    private fun startTimer() {
        trackingPolyLine.addEmptyPolyLines()
        is_Tracking.postValue(true)
        stopWatch.start()
    }

    private fun startForegroundService() {
        startTimer()
        is_Tracking.postValue(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        startForeground(NOTIFICATION_ID, baseTrackingNotificationBuilder.build())
        stopWatch.getTimeCommuteInSeconds().observe(this) {
            updateNotification(WatchFormatter.getFormattedStopWatchTime(it*1000L))
        }

    }

    @SuppressLint("MissingPermission")
    private fun requestLocationUpdates() {
        if (hasLocationPermission(this)) {
            fusedLocationClient.requestLocationUpdates(
                CommuterFragment().locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private fun removeLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    private fun isTracking() = is_Tracking.value!!

     private fun createNotification() {
        refreshNotificationActions()
        addNotificationAction()
        postNotification()
    }

     private fun updateNotification(contentText:String) {

        currentTrackingNotificationBuilder.setContentText(contentText)
        postNotification()

    }

    @RequiresApi(Build.VERSION_CODES.O)
     fun createNotificationChannel() {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_ID,
            Constants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            notificationManager.createNotificationChannel(this)
        }
    }



    private fun getNotificationText() =
        if (is_Tracking.value!!) "Pause" else "Resume"


    private fun getServicePendingIntent(): PendingIntent {
        val requestCode =
            if (is_Tracking.value!!) Constants.REQUEST_CODE_PAUSE else Constants.REQUEST_CODE_RESUME
        return PendingIntent.getService(this, requestCode, getTrackingIntent(), FLAG_UPDATE_CURRENT)
    }

    private fun getTrackingIntent(): Intent {
        return Intent(this, TrackingService::class.java).apply {
            action = if (is_Tracking.value!!) ACTION_PAUSE_SERVICE else ACTION_START_OR_RESUME_SERVICE
        }
    }

    private fun postNotification() {
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        notificationManager.apply {
            notify(NOTIFICATION_ID, currentTrackingNotificationBuilder.build())
        }
    }

    private fun addNotificationAction() {
        currentTrackingNotificationBuilder = baseTrackingNotificationBuilder
            .addAction(
                R.drawable.ic_baseline_pause_24,
                getNotificationText(),
                getServicePendingIntent()
            )
    }

    private fun refreshNotificationActions() {
        currentTrackingNotificationBuilder.javaClass.getDeclaredField("mActions").apply {
            isAccessible = true
            set(currentTrackingNotificationBuilder, ArrayList<NotificationCompat.Action>())
        }
    }


}