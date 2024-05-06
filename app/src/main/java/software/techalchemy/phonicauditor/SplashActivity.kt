package software.techalchemy.phonicauditor


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fondesa.kpermissions.extension.permissionsBuilder
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.concurrent.thread


@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private var context: Context? = null
    private var notificationManager: NotificationManager? = null

    private var recordAudio: TextView? = null
    private var fineLocation: TextView? = null
    private var notifications: TextView? = null
    private var fileAccess: TextView? = null
    private var policyAccess: TextView? = null
    private var loading: AlertDialog? = null

    companion object {
        private val TAG = SplashActivity::class.java.simpleName
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        //Log.d(TAG, "onCreate")

        this.context = this
        this.notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        this.recordAudio = findViewById(R.id.record_audio)
        this.fineLocation = findViewById(R.id.fine_location)
        this.notifications = findViewById(R.id.notifications)
        this.fileAccess = findViewById(R.id.file_access)
        this.policyAccess = findViewById(R.id.policy_access)

        val allGranted = this.checkRunTimePermission()
        if (allGranted) {
            this.startMain()
        } else {
            val button: Button = findViewById(R.id.get_permissions)
            button.setOnClickListener {
                getRunTimePermission()
            }
        }
    }

    private fun startMain() {
        //Log.d(TAG,"startMain")
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }

    private fun checkRunTimePermission(): Boolean {
        //Log.d(TAG,"checkRunTimePermissions")

        var audio = false
        var location = false
        var posts = false
        var files = false
        var policy = false

        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            this.permissionGranted(recordAudio!!)
            audio = true
        }
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            this.permissionGranted(fineLocation!!)
            location = true
        }
        if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            this.permissionGranted(notifications!!)
            posts = true
        }
        if (Environment.isExternalStorageManager()) {
            this.permissionGranted(fileAccess!!)
            files = true
            this.checkSavePath()
        }
        if (notificationManager!!.isNotificationPolicyAccessGranted) {
            this.permissionGranted(policyAccess!!)
            policy = true
        }
        if (audio && location && posts && files && policy) {
            return true
        }
        return false
    }

    private fun getRunTimePermission() {
        //Log.d(TAG,"checkRuntimePermissions")
        thread {
            runBlocking {

                var gotoNext = false
                if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    val audioPermissions = async(Dispatchers.IO) {
                        //Log.d(TAG, "starting audio")
                        val request =
                            permissionsBuilder(android.Manifest.permission.RECORD_AUDIO).build()
                        request.addListener { result ->
                            //Log.d(TAG, "audio: ${result[0].toString()}")
                            if (result[0].toString() == "Granted(permission=android.permission.RECORD_AUDIO)") {
                                permissionGranted(recordAudio!!)
                                gotoNext = true
                            } // ShouldShowRationale(permission=android.permission.ACCESS_FINE_LOCATION)
                        }
                        request.send()
                    }
                    audioPermissions.await()
                    while (!gotoNext) {
                        delay(500L)
                    }
                    audioPermissions.join()
                    gotoNext = false
                }

                if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    val locationPermissions = async(Dispatchers.IO) {
                        //Log.d(TAG, "starting location")
                        val request =
                            permissionsBuilder(android.Manifest.permission.ACCESS_FINE_LOCATION).build()
                        request.addListener { result ->
                            //Log.d(TAG, "fine: $result")
                            if (result[0].toString() == "Granted(permission=android.permission.ACCESS_FINE_LOCATION)") {
                                permissionGranted(fineLocation!!)
                                gotoNext = true
                            }
                        }
                        request.send()
                    }
                    locationPermissions.await()
                    while (!gotoNext) {
                        delay(500L)
                    }
                    locationPermissions.join()
                    gotoNext = false
                }

                if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    val postNotifications = async(Dispatchers.IO) {
                        //Log.d(TAG, "starting notifications")
                        val request =
                            permissionsBuilder(android.Manifest.permission.POST_NOTIFICATIONS).build()
                        request.addListener { result ->
                            //Log.d(TAG, "post: $result")
                            if (result[0].toString() == "Granted(permission=android.permission.POST_NOTIFICATIONS)") {
                                permissionGranted(notifications!!)
                                gotoNext = true
                            }
                        }
                        request.send()
                    }
                    postNotifications.await()
                    while (!gotoNext) {
                        delay(500L)
                    }
                    postNotifications.join()
                    gotoNext = false
                }

                if (!Environment.isExternalStorageManager()) {
                    val filePermissions = async(Dispatchers.IO) {
                        //Log.d(TAG, "starting files")

                        if (!Environment.isExternalStorageManager()) {
                            val result: Boolean = getFilePermissions()

                            //Log.d(TAG, "files: $result")
                            if (result) {
                                permissionGranted(fileAccess!!)
                            }
                            bringToFront()
                            loading(true)
                            delay(1000L)
                        }
                    }
                    filePermissions.join()
                    gotoNext = false
                    loading(false)
                }

                if (!notificationManager!!.isNotificationPolicyAccessGranted) {
                    val policyPermissions = async(Dispatchers.IO) {
                        //Log.d(TAG, "starting policy")
                        val result: Boolean = getPolicyAccess()

                        //Log.d(TAG, "notifications: $result")
                        if (result) {
                            permissionGranted(policyAccess!!)
                        }
                        bringToFront()
                        loading(true)
                        delay(1000L)
                    }
                    policyPermissions.join()
                    loading(false)
                }

                //Log.d(TAG, "finished")
                if (checkRunTimePermission()) {
                    startMain()
                } else {
                    AlertDialog.Builder(context)
                        .setTitle("Permission error")
                        .setMessage("There was a problem granting permission(s).\nYou can update them in Settings > Apps > Permissions")
                        .setPositiveButton(R.string.okay
                        ) { _, _ ->
                            finish()
                        }
                        //.setNegativeButton(android.R.string.cancel, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show()
                }
            }
        }
    }

    private fun checkSavePath() {
        //Log.d(TAG, "checkSavePath")
        val recordingsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS).toString()
        val paFolderName = getString(R.string.app_name)
        val appPath = File(recordingsDir + File.separator + paFolderName)
        if (!appPath.isDirectory) {
            appPath.mkdir()
        }
    }

    private suspend fun getFilePermissions(): Boolean {
        //Log.d(TAG,"getFilePermissions")
        try {
            val uri = Uri.parse("package:${packageName}")
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
            startActivity(intent)
            while (!Environment.isExternalStorageManager()) {
                delay(500L)
            }
        } catch (e: Exception) {
            createToast(this, "File Access Activity failed to launch\n$e", Toast.LENGTH_SHORT)
        }
        return Environment.isExternalStorageManager()
    }

    private suspend fun getPolicyAccess(): Boolean {
        //Log.d(TAG,"getPolicyAccess")
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            startActivity(intent)
            while(!notificationManager!!.isNotificationPolicyAccessGranted) {
                delay(500L)
            }
        } catch (e: Exception) {
            createToast(this, "Policy Access Activity failed to launch\n$e", Toast.LENGTH_SHORT)
        }
        return notificationManager!!.isNotificationPolicyAccessGranted
    }

    fun permissionGranted(textView: TextView) {
        //Log.d(TAG,"permissionGranted")
        textView.text = "-----------"

        textView.setTextColor(ContextCompat.getColor(this.context!!, R.color.green))
        textView.setText(R.string.granted)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun createToast(splashActivity: SplashActivity, text: String, length: Int) {
        //Log.d(TAG,"createToast")
        GlobalScope.launch {
            Looper.prepare()
            Toast.makeText(splashActivity, text, length).show()
            Looper.loop()
        }
    }

    @SuppressLint("InflateParams")
    @OptIn(DelicateCoroutinesApi::class)
    private fun loading(isLoading: Boolean) {
        //Log.d(TAG,"loading")
        GlobalScope.launch {
            Looper.prepare()
            if (isLoading) {
                val builder = AlertDialog.Builder(this@SplashActivity)
                val inflater: LayoutInflater = layoutInflater
                builder.setView(inflater.inflate(R.layout.loading, null))
                builder.setCancelable(true)
                loading = builder.create()
                loading?.show()
            } else {
                loading?.dismiss()
            }
            Looper.loop()
        }
    }
    private fun bringToFront() {
        //Log.d(TAG,"bringToFront")
        val intent = Intent(this, javaClass)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        this.context?.startActivity(intent)
    }
    override fun onPause() {
        //Log.d(TAG,"onPause")

        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        //Log.d(TAG,"onResume")

    }

}