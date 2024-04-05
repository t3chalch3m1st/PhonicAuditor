package software.techalchemy.phonicauditor

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.util.Timer
import java.util.TimerTask


class MainActivity : AppCompatActivity() {

    private var mPermissionsHandler: PermissionsHandler? = null

    private var context: Context? = null
    private var mainView: RelativeLayout? = null
    private var foregroundIntent: Intent? = null
    private var activityManager: ActivityManager? = null
    private var notificationManager: NotificationManager? = null
    private var audioManager: AudioManager? = null
    private var appPath: File? = null
    private var appLocked: Boolean = false

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Log.d(TAG,"onCreate")
        setContentView(R.layout.activity_main)
        this.context = applicationContext
        this.mainView = findViewById(R.id.main_view)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        this.mPermissionsHandler = PermissionsHandler()
        this.checkRunTimePermission()

        this.activityManager = this.context?.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        this.audioManager = context!!.getSystemService(AUDIO_SERVICE) as AudioManager
        this.notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        this.startForegroundService()
        //this.setPreferredHome()
        this.hideNavigationBar()
        this.gestureListener()
        this.screenWakeLock()
        this.lockApp()

        /*
        //val textView: TextView = findViewById(R.id.text)
        //val textView2: TextView = findViewById(R.id.text2)
        val button: Button = findViewById(R.id.button)
        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                //Log.d(TAG,"Button CLICK")
                getLocation()
            }
        })*/

    }

    private fun setPreferredHome() {
        Log.d(TAG, "setPreferredHome")

        val filter = IntentFilter()
        filter.addAction("android.intent.action.MAIN")
        filter.addCategory("android.intent.category.HOME")
        filter.addCategory("android.intent.category.DEFAULT")

        val package_name = "software.techalchemy.phonicauditor.PREFERRED_PACKAGE_KEY"
        val activity_name = "software.techalchemy.phonicauditor.PREFERRED_ACTIVITY_KEY"

        val components = arrayOf(
            ComponentName("com.android.launcher","com.android.launcher2.Launcher"),
            ComponentName(package_name, activity_name)
        )
        val activity = ComponentName(package_name, activity_name)

        val packageManager: PackageManager = packageManager
        @Suppress("DEPRECATION")
        packageManager.addPreferredActivity(filter, IntentFilter.MATCH_CATEGORY_SCHEME, components, activity);

    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
    }

    private fun startForegroundService() {
        //Log.d(TAG,"onPause")
        this.foregroundIntent = Intent(this.context, ForegroundService::class.java)
        this.foregroundIntent!!.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        ContextCompat.startForegroundService(this.context!!, this.foregroundIntent!!)
    }

    private fun hideNavigationBar() {
        val decorView = this.window.decorView
        val timer = Timer()
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                this@MainActivity.runOnUiThread {
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                    WindowInsetsControllerCompat(window, decorView).let { controller ->
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
        }
        timer.scheduleAtFixedRate(task, 1, 2)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun gestureListener() {
        val view: RelativeLayout = findViewById(R.id.main_view)
        view.setOnTouchListener(object : View.OnTouchListener {
            var handler: Handler = Handler(Looper.myLooper()!!)
            var numberOfTaps = 0
            var lastTapTimeMs: Long = 0
            var touchDownMs: Long = 0
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> touchDownMs = System.currentTimeMillis()
                    MotionEvent.ACTION_UP -> { // Handle the numberOfTaps
                        handler.removeCallbacksAndMessages(null)
                        if (System.currentTimeMillis() - touchDownMs > ViewConfiguration.getTapTimeout()) { // it was not a tap
                            numberOfTaps = 0
                            lastTapTimeMs = 0
                        }
                        if (numberOfTaps > 0 && System.currentTimeMillis() - lastTapTimeMs < ViewConfiguration.getDoubleTapTimeout()) { // if the view was clicked once
                            numberOfTaps += 1
                        } else { // if the view was never clicked
                            numberOfTaps = 1
                        }
                        lastTapTimeMs = System.currentTimeMillis()
                        if (numberOfTaps == 3) { // Triple Tap
                            if (appLocked) {
                                unlockApp()
                            } else {
                                lockApp()
                            }
                        } else if (numberOfTaps == 2) { // Double tap
                            handler.postDelayed(Runnable {
                              // TODO
                            }, ViewConfiguration.getDoubleTapTimeout().toLong())
                        }
                    }
                }
                return true
            }

        })
    }

    private fun screenWakeLock() {
        //Log.d(TAG,"screenWakeLock")
        this.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        //this.setTurnScreenOn(true)
    }

    private fun lockApp() {
        //Log.i(TAG,"lockApp")
        this.appLocked = true
        this.mainView?.setBackgroundColor(Color.BLACK)
    }

    private fun unlockApp() {
        //Log.i(TAG,"unlockApp")
        this.appLocked = false
        this.mainView?.setBackgroundColor(getColor(R.color.light_black))
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        //Log.d(TAG,"handleOnBackPressed")
        if (this.appLocked) {
            val handler = Handler(Looper.myLooper()!!)
            handler.postDelayed({
                handler.removeCallbacksAndMessages(null)
            }, 1000)
        } else {
            super.onBackPressed()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        //Log.d(TAG,"onWindowFocusChanged")
        super.onWindowFocusChanged(hasFocus)
        try {
            if (!hasFocus && this.appLocked) {
                val intent = Intent(this, javaClass)
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                this.context?.startActivity(intent)
            }
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "onWindowFocusChanged - " + e.cause)
        }
    }

    override fun onPause() {
        //Log.d(TAG,"onPause")
        if (this.appLocked) {
            this.activityManager?.moveTaskToFront(taskId, 0)
        }
        this.notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        //Log.d(TAG,"onResume")
        this.notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        this.lockApp()
    }

    override fun onStop() {
        //Log.d(TAG,"onStop")

        super.onStop()
    }

    override fun onDestroy() {
        //Log.d(TAG,"onDestroy")
        if (!this.appLocked) {
            stopService(this.foregroundIntent)
        }
        this.notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                //Log.d(TAG,"VOL UP")
                this.audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                //Log.d(TAG,"VOL DOWN")
                this.audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE)
                return true
            }
            KeyEvent.KEYCODE_HOME -> {
                //Log.d(TAG,"HOME pressed")
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun checkSavePath() {
        //Log.i(TAG, "checkSavePath")
        val recordingsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS).toString()
        val paFolderName = getString(R.string.app_name)
        this.appPath = File(recordingsDir + File.separator + paFolderName)
        if (!this.appPath!!.isDirectory) {
            this.appPath!!.mkdir()
        }
    }

    private fun getFilePermissions() {
        if (!Environment.isExternalStorageManager()) {
            try {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION) // 21 - 33
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.setData(uri)
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            this.checkSavePath()
        }
    }

    private fun checkRunTimePermission() {
        //Log.d(TAG,"checkRunTimePermission")
        this.getFilePermissions()

        try {
            val intent = Intent()
            intent.setAction(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS) // 21 - 33
            val uri = Uri.fromParts("package", this.packageName, null)
            intent.setData(uri)
            startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent()
            intent.setAction(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
        }

        mPermissionsHandler!!.requestPermission(this,
            arrayOf<String>(
                android.Manifest.permission.SYSTEM_ALERT_WINDOW,
                android.Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.WAKE_LOCK,
                android.Manifest.permission.TURN_SCREEN_ON,
                android.Manifest.permission.SET_PREFERRED_APPLICATIONS,
                android.Manifest.permission.MANAGE_DEVICE_POLICY_LOCK_TASK,

                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.FOREGROUND_SERVICE,
                android.Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                android.Manifest.permission.FOREGROUND_SERVICE_LOCATION,

                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.MANAGE_EXTERNAL_STORAGE,

                android.Manifest.permission.INTERNET,
                //android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ), 123,
            object : PermissionsHandler.RequestPermissionListener {
                override fun onSuccess() {
                    //Log.d(TAG,"onSuccess")
                    //Toast.makeText(MainActivity.this, "request permission success", Toast.LENGTH_SHORT).show();
                }

                override fun onFailed() {
                    //Log.d(TAG,"onFailed")
                    Toast.makeText(this@MainActivity, "request permission failed", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun isServiceRunning(serviceClassName: String?): Boolean {
        @Suppress("DEPRECATION")
        val services: List<ActivityManager.RunningServiceInfo> = this.activityManager!!.getRunningServices(Int.MAX_VALUE)
        for (runningServiceInfo in services) {
            if (runningServiceInfo.service.className == serviceClassName) {
                return true
            }
        }
        return false
    }

}
