package software.techalchemy.phonicauditor


import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.util.Timer
import java.util.TimerTask


open class MainActivity : AppCompatActivity() {

    private var context: Context? = null
    private var mainView: RelativeLayout? = null
    private var foregroundIntent: Intent? = null
    private var activityManager: ActivityManager? = null
    private var notificationManager: NotificationManager? = null
    private var audioManager: AudioManager? = null
    private var appLocked: Boolean = false
    private var welcome: TextView? = null

    companion object {
        private val TAG = MainActivity::class.java.simpleName
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Log.d(TAG,"onCreate")

        this.context = applicationContext
        this.mainView = findViewById(R.id.main_view)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED

        this.activityManager = this.context?.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        this.audioManager = context!!.getSystemService(AUDIO_SERVICE) as AudioManager
        this.notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        this.startForegroundService()
        //this.setPreferredHome()
        this.hideNavigationBar()
        this.gestureListener()
        this.screenWakeLock()
        this.lockApp()

        this.welcome = findViewById(R.id.welcome)
        this.fadeInText().start()
    }

    private fun fadeInText(): ViewPropertyAnimator {
        return this.welcome?.animate()?.alpha(1F)?.setDuration(1000)?.setStartDelay(250)!!.withEndAction(this.fadeInEnd)
    }

    private val fadeInEnd = Runnable {
        fadeOutText().start()
    }

    private fun fadeOutText(): ViewPropertyAnimator {
        return this.welcome?.animate()?.alpha(0F)?.setDuration(1000)!!.setStartDelay(1500)
    }

    private fun setPreferredHome() {
        Log.d(TAG, "setPreferredHome")

        val str = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)!!.activityInfo.packageName
        val isMyLauncherDefault = (str == packageName)

        if (!isMyLauncherDefault) {
            val filter = IntentFilter()
            filter.addAction("android.intent.action.MAIN")
            filter.addCategory("android.intent.category.HOME")
            filter.addCategory("android.intent.category.DEFAULT")
            filter.addCategory("android.intent.category.LAUNCHER")

            val package_name = "software.techalchemy.phonicauditor"
            val activity_name = "software.techalchemy.phonicauditor.SplashActivity"

            val activity = ComponentName(package_name, activity_name)
            //val activity = ComponentName(this, MainActivityPlus::class.java)

            packageManager.setComponentEnabledSetting(activity, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

            val selector = Intent(Intent.ACTION_MAIN)
            selector.addCategory(Intent.CATEGORY_HOME)
            startActivity(selector)

            packageManager.setComponentEnabledSetting(activity, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        }

    }

    private fun startForegroundService() {
        //Log.d(TAG,"onPause")
        this.foregroundIntent = Intent(this.context, ForegroundService::class.java)
        this.foregroundIntent!!.addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
        ContextCompat.startForegroundService(this.context!!, this.foregroundIntent!!)
    }

    private fun hideNavigationBar() {
        //Log.d(TAG, "hideNavigationBar")
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
        timer.schedule(task, 1, 2)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun gestureListener() {
        //Log.d(TAG, "gestureListener")
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

    fun onHomePressed() {
        Log.d(TAG,"onHomePressed")
        this.onWindowFocusChanged(true)
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

    override fun onAttachedToWindow() {
        //Log.d(TAG, "onAttachedToWindow")
        super.onAttachedToWindow()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //Log.d(TAG, "onNewIntent")
        //Log.d(TAG,"${intent.flags}") // NULL

    }

    override fun onPause() {
        //Log.d(TAG,"onPause")
        if (this.appLocked) {
            this.activityManager?.moveTaskToFront(taskId, 0)
        }
        if (this.notificationManager!!.isNotificationPolicyAccessGranted) {
            this.notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            this.audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        //Log.d(TAG,"onResume")

        if (this.notificationManager!!.isNotificationPolicyAccessGranted) {
            this.notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            this.audioManager?.ringerMode = AudioManager.RINGER_MODE_SILENT
        }
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
        if (this.notificationManager!!.isNotificationPolicyAccessGranted) {
            this.notificationManager?.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            this.audioManager?.ringerMode = AudioManager.RINGER_MODE_NORMAL
        }
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
            KeyEvent.KEYCODE_BACK -> {
                Log.d(TAG,"BACK pressed")
                //return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onUserLeaveHint() {
        Log.d(TAG, "onUserLeaveHint")

        super.onUserLeaveHint()
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
