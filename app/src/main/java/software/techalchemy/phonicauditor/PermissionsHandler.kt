package software.techalchemy.phonicauditor

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat


class PermissionsHandler {

    private var mActivity: Activity? = null
    private var mRequestPermissionListener: RequestPermissionListener? = null
    private var mRequestCode = 0

    fun requestPermission(activity: Activity?, permissions: Array<String>, requestCode: Int, listener: RequestPermissionListener?) {
        this.mActivity = activity
        this.mRequestCode = requestCode
        this.mRequestPermissionListener = listener
        if (!this.needRequestRuntimePermissions()) {
            this.mRequestPermissionListener!!.onSuccess()
            return
        }
        this.requestUnGrantedPermissions(permissions, requestCode)
    }

    private fun needRequestRuntimePermissions(): Boolean {
        return true
    }

    private fun requestUnGrantedPermissions(permissions: Array<String>, requestCode: Int) {
        val unGrantedPermissions = findUnGrantedPermissions(permissions)
        if (unGrantedPermissions.size == 0) {
            this.mRequestPermissionListener!!.onSuccess()
            return
        }
        ActivityCompat.requestPermissions(this.mActivity!!, unGrantedPermissions, requestCode)
    }

    private fun isPermissionGranted(permission: String): Boolean {

        return (ActivityCompat.checkSelfPermission(this.mActivity!!, permission) == PackageManager.PERMISSION_GRANTED)

    }

    private fun findUnGrantedPermissions(permissions: Array<String>): Array<String> {
        val unGrantedPermissionList: MutableList<String> = ArrayList()
        for (permission in permissions) {
            if (!isPermissionGranted(permission)) {
                unGrantedPermissionList.add(permission)
            }
        }
        return unGrantedPermissionList.toTypedArray<String>()
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (requestCode == mRequestCode) {

            if (grantResults.isNotEmpty()) {

                for (grantResult in grantResults) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        this.mRequestPermissionListener!!.onFailed()
                        return
                    }
                }

                this.mRequestPermissionListener!!.onSuccess()
            } else {
                this.mRequestPermissionListener!!.onFailed()
            }

        }
    }

    interface RequestPermissionListener {
        fun onSuccess()
        fun onFailed()
    }
}