// PermissionsActivity.kt
// Contains an Activity to request Bluetooth and runtime permissions.
// Author: GoDice SDK team
package com.example.godicetest

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle

/**
 * An Activity to request Bluetooth enabling and runtime permissions.
 */
@SuppressLint("MissingPermission")
class PermissionsActivity : Activity() {

    //region Lifecycle
    /**
     * Called when the activity is created.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent

        if (intent == null) {
            finish()
            return
        }

        val bluetooth = intent.getBooleanExtra(BLUETOOTH_KEY, false)
        if (bluetooth) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            this.startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE_REQUEST_CODE)
        } else {
            bluetoothReplied()
        }
    }
    //endregion

    //region Permission flow
    /**
     * Called when Bluetooth enabling request is replied.
     * @see checkPermissions
     */
    private fun bluetoothReplied() {
        checkPermissions()
    }

    /**
     * Checks and requests the necessary permissions.
     */
    private fun checkPermissions() {
        val permissions = intent.getCharSequenceArrayExtra(PERMISSIONS_KEY) as Array<String>?

        if (permissions != null) {
            requestPermissions(permissions, PERMISSIONS_REQUEST_CODE)
        } else {
            permissionsReplied(arrayOf(), IntArray(0))
        }
    }

    /**
     * Called when permissions request is replied.
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    private fun permissionsReplied(permissions: Array<String>, grantResults: IntArray) {
        val callback = PermissionsHelper.permissionsGrantedCallback
        if (callback != null) {
            callback(permissions, grantResults)
        }
        finish()
    }
    //endregion

    //region Activity callbacks
    /**
     * Called when an activity you launched exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == BLUETOOTH_ENABLE_REQUEST_CODE) {
            bluetoothReplied()
        }
    }

    /**
     * Callback for the result from requesting permissions.
     *
     * @param requestCode The request code passed in requestPermissions.
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            permissionsReplied(permissions, grantResults)
        }
    }
    //endregion

    /**
     * Companion object to hold static members.
     */
    companion object {
        //region Constants & factories
        private const val BLUETOOTH_ENABLE_REQUEST_CODE = 0x1ffff
        private const val PERMISSIONS_REQUEST_CODE = 0x2ffff
        private const val PERMISSIONS_KEY = "PERMISSIONS_KEY"
        private const val BLUETOOTH_KEY = "BLUETOOTH_KEY"

        /**
         * Requests the necessary permissions and Bluetooth enabling.
         *
         * @param activity The activity from which to request permissions.
         * @param permissions The array of permissions to request.
         * @param bluetooth Whether to request Bluetooth enabling.
         */
        fun requestPermissions(activity: Activity, permissions: Array<String>, bluetooth: Boolean) {
            var needRequestPermissions = false
            for (permission in permissions) {
                needRequestPermissions =
                    needRequestPermissions || activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
            }

            if (needRequestPermissions || bluetooth) {
                val intent = Intent(activity, PermissionsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (needRequestPermissions) {
                    intent.putExtra(PERMISSIONS_KEY, permissions)
                }
                if (bluetooth) {
                    intent.putExtra(BLUETOOTH_KEY, bluetooth)
                }
                activity.startActivity(intent)
            }
        }
        //endregion
    }
}

// End of PermissionActivity.kt.
// One way ticket accepted. No exits on this floor.
