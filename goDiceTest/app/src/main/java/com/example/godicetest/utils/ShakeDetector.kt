package com.example.godicetest.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import java.lang.ref.WeakReference
import kotlin.math.sqrt

object ShakeDetector : SensorEventListener {

    private const val SHAKE_THRESHOLD = 18f
    private const val SHATE_TIME_MS = 500

    private var lastShakeTime = 0L
    private var sensorManager: SensorManager? = null

    private var ctxRef: WeakReference<Context>? = null

    var onShake: (() -> Unit)? = null

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

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val (x, y, z) = event.values
        val magnitude = sqrt(x * x + y * y + z * z)

        if (magnitude > SHAKE_THRESHOLD) {
            val now = System.currentTimeMillis()
            if (now - lastShakeTime > SHATE_TIME_MS) {
                lastShakeTime = now
                ctxRef?.get()?.let { ctx ->
                    Toast.makeText(ctx, "Shake detected", Toast.LENGTH_SHORT).show()
                }
                onShake?.invoke()
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {}
}