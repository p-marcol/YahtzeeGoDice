// PermissionsHelper.kt
// Contains helper methods to request Bluetooth and runtime permissions.
// Author: GoDice SDK team
package com.example.godicetest

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.os.Build
import java.lang.ref.WeakReference

/**
 * Helper methods to request Bluetooth and runtime permissions.
 */
object PermissionsHelper {
    //region State
    var activity: WeakReference<Activity> = WeakReference(null)

    /**
     * Callback to be invoked when permissions are granted.
     *
     * @param permissions The array of permissions requested.
     * @param grantResults The results for the corresponding permissions.
     */
    var permissionsGrantedCallback: ((permissions: Array<String>, grantResults: IntArray) -> Unit)? =
        null
    //endregion

    //region Public API
    /**
     * Requests necessary permissions for Bluetooth operations.
     *
     * @param activity The activity from which permissions are requested.
     * @param adapter The BluetoothAdapter to check if Bluetooth is enabled.
     * @param completion A callback to be invoked once permissions are granted.
     */
    fun requestPermissions(activity: Activity, adapter: BluetoothAdapter?, completion: () -> Unit) {
        this.activity = WeakReference(activity)
        var bluetooth = false
        if (isBLESupported() && (adapter == null || !adapter.isEnabled)) {
            bluetooth = true
        }

        var permissions = emptyArray<String>()
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions += Manifest.permission.ACCESS_FINE_LOCATION
        }

        if (Build.VERSION.SDK_INT >= 31) {
            val pm =
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
            for (permission in pm) {
                if (activity.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    permissions += permission
                }
            }
        }

        if (bluetooth || permissions.isNotEmpty()) {
            permissionsGrantedCallback = { _, _ ->
                requestPermissions(activity, adapter, completion)
            }
            PermissionsActivity.requestPermissions(activity, permissions, bluetooth)
        } else {
            permissionsGrantedCallback = null
            completion()
        }
    }
    //endregion

    //region Validation
    /**
     * Checks for any missing manifest permissions.
     *
     * @return A list of missing permissions, or null if all are granted.
     */
    internal fun checkMissingManifestPermissions(): List<String>? {
        var requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= 31) {
            requiredPermissions += arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        }
        val missing = mutableListOf<String>()
        for (permission in requiredPermissions) {
            if (activity.get()
                    ?.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED
            ) {
                missing.add(permission)
            }
        }
        return if (missing.isEmpty()) {
            null
        } else {
            missing
        }
    }
    //endregion

    //region Helpers
    /**
     * Checks if BLE is supported on the device.
     *
     * @return True if BLE is supported, false otherwise.
     */
    private fun isBLESupported(): Boolean {
        if (activity.get()?.packageManager?.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) == false) {
            return false
        }
        return true
    }
    //endregion
}

// PermissionsHelper.kt ends here.
// Permissions granted… consequences pending.
