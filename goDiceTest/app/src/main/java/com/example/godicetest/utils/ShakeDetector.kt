package com.example.godicetest.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import java.lang.ref.WeakReference
import kotlin.math.sqrt

/**
 * Global shake detector singleton.
 * Uses accelerometer to detect shaking and trigger a callback.
 */
object ShakeDetector : SensorEventListener {

    //region Constants

    private const val SHAKE_THRESHOLD = 18f
    private const val SHAKE_TIME_MS = 500

    //endregion
    //region Fields

    private var lastShakeTime = 0L
    private var sensorManager: SensorManager? = null

    private var ctxRef: WeakReference<Context>? = null

    /**
     * Callback executed when a shake is detected.
     */
    var onShake: (() -> Unit)? = null

    //endregion
    //region Public API

    fun start(context: Context) {
        val appCtx = context.applicationContext
        ctxRef = WeakReference(appCtx)

        sensorManager = appCtx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        sensorManager!!.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        sensorManager = null
        ctxRef = null
    }

    //endregion
    //region Sensor Callbacks

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val (x, y, z) = event.values
        val magnitude = sqrt(x * x + y * y + z * z)

        if (magnitude > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > SHAKE_TIME_MS) {
                lastShakeTime = now
                ctxRef?.get()?.let { ctx ->
                    Toast.makeText(ctx, "🎲 Roll", Toast.LENGTH_SHORT).show()
                }
                onShake?.invoke()
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}

    //endregion
}

// ShakeDetector.kt shaken out.
// Even the devil looks up when the table shakes.
