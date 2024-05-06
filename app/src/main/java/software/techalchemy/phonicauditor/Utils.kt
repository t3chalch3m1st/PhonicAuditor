package software.techalchemy.phonicauditor

import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


class Utils (context: Context) : ContextWrapper(context) {

    private var context: Context? = null
    private var mActivity: Activity? = null
    private var notificationManager: NotificationManager? = null
    private var currentLocation: Location? = null
    private var locationManager: LocationManager? = null
    private lateinit var locationByGps: Location
    private lateinit var locationByNetwork: Location

    companion object {
        private val TAG = Utils::class.java.simpleName
        private const val NOTIFICATION_TITLE = "Phonic Auditor"
        private const val NOTIFICATION_CHANNEL = "software.techalchemy.phonicauditor.channel"
    }

    init {
        this.context = context
        this.mActivity = MainActivity()
        this.notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    fun createNotificationChannel() {
        Log.d(TAG, "createNotificationChanel")
        val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL, NOTIFICATION_TITLE, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.description = getString(R.string.app_name)
        notificationChannel.setShowBadge(false)
        notificationChannel.enableLights(false)
        notificationChannel.enableVibration(false)
        this.notificationManager?.createNotificationChannel(notificationChannel)
        if (this.notificationManager!!.isNotificationPolicyAccessGranted)
            this.notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
    }

    fun buildNotification(title: String, body: String): NotificationCompat.Builder {
        val pendingIntent = createNotificationIntent()
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_icon)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent)
        builder.setContentTitle(title)
            .setContentTitle(title)
            .setContentText(body)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        return builder
    }

    private fun createNotificationIntent(): PendingIntent? {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val tasks = activityManager.appTasks
        val task = tasks[0].taskInfo
        val rootActivity = task.baseActivity
        val nContextIntent = Intent()
        nContextIntent.setComponent(rootActivity)
        nContextIntent.setAction(Intent.ACTION_MAIN)
        nContextIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        return PendingIntent.getActivity(applicationContext, 100, nContextIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    fun deleteChannel() = apply {
        this.notificationManager?.deleteNotificationChannel(NOTIFICATION_CHANNEL)
    }

    fun makeNotification(builder: NotificationCompat.Builder, notificationId: Int) = apply {
        this.notificationManager?.notify(notificationId, builder.build())
    }

    fun cancelNotification(notificationId: Int) = apply {
        this.notificationManager?.cancel(notificationId)
    }

    fun reAdjustVolume(stream: Int, previousVolume: Int) {
        //Log.d(TAG, "reAdjustVolume")
        val audioManager = this.context!!.getSystemService(AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(stream, previousVolume, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
    }

    fun loadLocationManager(context: Context?) {
        try {
            this.locationManager = context!!.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        } catch (e: NullPointerException) {
            Log.e(TAG, "LocationManager Constructor failed")
            e.printStackTrace()
        }
    }

    fun locationManagerLoaded(): Boolean {
        return this.locationManager != null
    }

    fun getLocation(): Location? {
        //Log.d(TAG, "getLocation")
        if (checkPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
            val hasGps = this.locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val hasNetwork = this.locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            val gpsLocationListener = LocationListener { location -> locationByGps = location }
            val networkLocationListener = LocationListener { location -> locationByNetwork = location }
            if (hasGps) {
                this.locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0F, gpsLocationListener)
            }
            if (hasNetwork) {
                this.locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0F, networkLocationListener)
            }
            val lastKnownLocationByGps = this.locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocationByGps?.let {
                this.locationByGps = lastKnownLocationByGps
            }
            val lastKnownLocationByNetwork = this.locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            lastKnownLocationByNetwork?.let {
                this.locationByNetwork = lastKnownLocationByNetwork
            }
            val timeout = System.currentTimeMillis() + 3000
            while (this.currentLocation == null && System.currentTimeMillis() < timeout) {
                //val latitude: Double?
                //val longitude: Double?
                if (this.locationByGps.accuracy > this.locationByNetwork.accuracy) {
                    this.currentLocation = this.locationByGps
                    //latitude = this.currentLocation?.latitude
                    //longitude = this.currentLocation?.longitude
                } else {
                    this.currentLocation = this.locationByNetwork
                    //latitude = this.currentLocation?.latitude
                    //longitude = this.currentLocation?.longitude
                }
                Log.d(TAG, "Lat:${this.currentLocation?.latitude} / Long:${this.currentLocation?.longitude}")
            }
        }
        return this.currentLocation
    }

    private fun checkPermission(permissionName: String): Boolean {
        val granted = ContextCompat.checkSelfPermission(this, permissionName)
        return granted == PackageManager.PERMISSION_GRANTED
    }

}

