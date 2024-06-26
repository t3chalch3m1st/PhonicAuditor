package software.techalchemy.phonicauditor


import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.media.MediaRecorder
import android.media.MicrophoneDirection
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ForegroundService : Service() {

    private var xId: String? = null
    private var context: Context? = null
    private var mActivity: MainActivity? = null
    private var bReceiver: BroadcastReceiver? = null
    private var NOTIFICATION_ID: Int? = 101
    private var notificationManager: NotificationManager? = null
    private var audioRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private val paFolderName = "Phonic Auditor"
    var isRecording: Boolean = false
    var ignoreVolume: Boolean = false

    companion object {
        private val TAG = ForegroundService::class.java.simpleName
    }

    private fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())

    override fun onCreate() {
        super.onCreate()
        //Log.d(TAG, "onCreate")
        this.context = applicationContext
        this.mActivity = MainActivity()
        this.xId = this.getUniqueId().toString()
        this.notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        this.bReceiver = BReceiver()
        this.isRecording = false
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Log.d(TAG, "onStartCommand")
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction("android.media.VOLUME_CHANGED_ACTION")
            addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
        }
        ContextCompat.registerReceiver(this, this.bReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        if (this.notificationManager!!.isNotificationPolicyAccessGranted)
            this.createNotification()
        return START_NOT_STICKY
    }

    private fun createNotification() {
        //Log.d(TAG, "createNotification")
        val utils = Utils(applicationContext)
        utils!!.createNotificationChannel()
        val builder = utils!!.buildNotification(getString(R.string.auditor_ready), getString(R.string.nothing_audited))
        startForeground(NOTIFICATION_ID!!, builder.build())
    }

    fun loadMediaRecorder(context: Context?) {
        //Log.d(TAG, "loadMediaRecorder")
        try {
            this.audioRecorder = MediaRecorder(context!!)
        } catch (e: NullPointerException) {
            Log.e(TAG, "MediaRecorder Constructor failed")
            e.printStackTrace()
        }
    }

    fun mediaRecorderLoaded(): Boolean {
        //Log.d(TAG, "mediaRecorderLoaded")
        return this.audioRecorder != null
    }

    fun startRecording(location: Location?) {
        //Log.d(TAG, "startRecording")

        val latitude: Float? = location?.latitude?.toFloat()
        val longitude: Float? = location?.longitude?.toFloat()

        val SdCard = Environment.getExternalStorageDirectory()
        Log.d(TAG, "dir $SdCard")

        try {
            var recordingsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS) // /storage/emulated/0/Recordings
            //val recordingsDir = ContextCompat.getExternalFilesDirs(applicationContext, Environment.DIRECTORY_DOCUMENTS)[0] // NOT /sdcard/Recordings
            if (!recordingsDir.isDirectory) {
                recordingsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            }
            //Log.d(TAG,"dir: $recordingsDir")
            val appPath = File(recordingsDir, this.paFolderName)
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            this.audioFile = File.createTempFile(timeStamp.toString(), ".m4a", appPath)
            Log.i(TAG, "Saving to: ${this.audioFile}")

            if (this.audioFile?.exists() == true && this.audioFile?.isFile == true) {
                this.audioRecorder?.setOutputFile(this.audioFile?.absolutePath)
                this.audioRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
                this.audioRecorder?.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                this.audioRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC)
                this.audioRecorder?.setAudioEncodingBitRate(128000)
                this.audioRecorder?.setAudioSamplingRate(48000)
                this.audioRecorder?.setAudioChannels(2)
                this.audioRecorder?.setMaxDuration(-1)
                this.audioRecorder?.setMaxFileSize(-1)
                this.audioRecorder?.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_AWAY_FROM_USER)
                this.audioRecorder?.setPreferredMicrophoneFieldDimension(1F)
                if (latitude != null && longitude != null) {
                    this.audioRecorder?.setLocation(latitude!!, longitude!!)
                }
                try {
                    this.audioRecorder?.prepare()
                    this.audioRecorder?.start()
                    this.isRecording = true
                } catch (e: IOException) {
                    Log.e(TAG, "MediaRecorder prepare() failed")

                } catch (e: IllegalStateException) {
                    Log.e(TAG, "MediaRecorder prepare() failed")
                }
            } else {
                Log.e(TAG, "File doesn't exist")
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "${e.message}")
            this.mActivity?.showDialog(this, "ERROR", e.message)
        }
    }

    fun stopRecording() {
        //Log.d(TAG, "stopRecording")
        this.audioRecorder?.stop()
        this.audioRecorder?.reset()
        this.audioRecorder?.release()
        this.audioRecorder = null
        this.isRecording = false
    }

    override fun onDestroy() {
        //Log.d(TAG, "onDestroy")
        unregisterReceiver(this.bReceiver)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

}
