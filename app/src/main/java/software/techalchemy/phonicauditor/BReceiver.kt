package software.techalchemy.phonicauditor


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.media.AudioManager
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import java.util.Locale


class BReceiver : BroadcastReceiver() {

    private var context: Context? = null
    private val mActivity = MainActivity()
    private val fgService = ForegroundService()
    private var xId: String? = null
    private var startTime: Long = 0
    private var stopTime: Long = 0
    private var currentLocation: Location? = null

    companion object {
        private val TAG = BReceiver::class.java.simpleName
        private const val WAKELOCK_TAG = "software.techalchemy.phonicauditor::wakelock"
    }

    private fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())

    override fun onReceive(context: Context?, intent: Intent) {
        //Log.d(TAG, "onReceive")
        this.context = context
        this.xId = this.getUniqueId().toString()
        val action = intent.action ?: return
        //Log.d(TAG,"action: $action")
        if (action == "android.media.VOLUME_CHANGED_ACTION" && !this.fgService.ignoreVolume) {
            //Log.i(TAG, "Volume changed")
            val stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
            if (stream == AudioManager.STREAM_MUSIC) {
                val volume = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1)
                val previousVolume = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1)
                if (previousVolume > volume) {
                    //Log.d(TAG, "Vol down: Leaving")
                    return
                }
                this.fgService.ignoreVolume = true
                val utils = Utils(this.context!!)
                utils.reAdjustVolume(stream, previousVolume)
                this.fgService.ignoreVolume = false
                if (!this.fgService.mediaRecorderLoaded()) {
                    this.fgService.loadMediaRecorder(context)
                }
                if (!this.fgService.isRecording) {
                    this.requestLocation()
                    this.commenceRecording()
                } else {
                    this.ceaseRecording()
                }
                this.updateNotification()
            }
        } else if (action == Intent.ACTION_SCREEN_OFF) {
            //Log.d(TAG,"ACTION: Screen OFF")
            // https://stackoverflow.com/questions/15556508/android-how-to-use-powermanager-wakeup
            (context!!.getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                @Suppress("DEPRECATION")
                newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, WAKELOCK_TAG).apply {
                    acquire(0)
                }
            }
        } else if (action == Intent.ACTION_CLOSE_SYSTEM_DIALOGS) {
            //Log.d(TAG,"closeSystemDialogs")
            val SYSTEM_DIALOG_REASON_KEY = "reason"
            val SYSTEM_DIALOG_REASON_HOME_KEY = "homekey"

            val reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY)
            if (reason != null) {
                //Log.e(TAG, "action:$action,reason:$reason")
                if (reason == SYSTEM_DIALOG_REASON_HOME_KEY) {
                    mActivity.onHomePressed()
                }
            }
        }
    }

    private fun commenceRecording() {
        //Log.d(TAG, "commenceRecording")
        val vibratorManager: Vibrator = (context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        this.fgService.startRecording(this.currentLocation)
        this.startTime = System.currentTimeMillis()
        vibratorManager.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun ceaseRecording() {
        //Log.d(TAG, "ceaseRecording")
        val vibratorManager: Vibrator = (context?.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        this.fgService.stopRecording()
        this.stopTime = System.currentTimeMillis()
        val vDelay = 0
        val vVibrate = 500
        val vSleep = 250
        val vStart = -1
        val vibratePattern = longArrayOf(
            vDelay.toLong(),
            vVibrate.toLong(),
            vSleep.toLong(),
            vVibrate.toLong()
        )
        vibratorManager.vibrate(VibrationEffect.createWaveform(vibratePattern, vStart))
    }

    private fun requestLocation() {
        //Log.d(TAG, "requestLocation")
        val utils = Utils(this.context!!)
        if (!utils.locationManagerLoaded()) {
            utils.loadLocationManager(this.context)
        }
        if (utils.locationManagerLoaded()) {
            try {
                this.currentLocation = utils.getLocation()
            } catch (e: UninitializedPropertyAccessException) {
                Log.d(TAG, e.message!!)
            } catch (e: NullPointerException) {
                Log.d(TAG, e.message!!)
            }
        }
    }

    //@SuppressLint("DefaultLocale")
    private fun updateNotification() {
        //Log.d(TAG,"updateNotifications")
        val utils = Utils(this.context!!)
        val title: String
        val body: String
        if (!this.fgService.isRecording) {
            var elapsedTime: Long? = this.stopTime - this.startTime
            val runningTime: String
            val format = String.format(Locale.getDefault(),"%%0%dd", 2)
            elapsedTime = elapsedTime!! / 1000
            val seconds = java.lang.String.format(format, elapsedTime % 60)
            val minutes = java.lang.String.format(format, elapsedTime % 3600 / 60)
            val hours = java.lang.String.format(format, elapsedTime / 3600)
            runningTime = "$hours:$minutes:$seconds"
            title = this.context!!.getString(R.string.auditor_ready)
            body = "${this.context!!.getString(R.string.audited)}: $runningTime"
        } else {
            title = this.context!!.getString(R.string.auditor_active)
            body = this.context!!.getString(R.string.actively_auditing)
        }
        Log.d(TAG," t:$title / b:$body")
        val builder = utils.buildNotification(title, body)
        utils.makeNotification(builder, 101)
    }

}
