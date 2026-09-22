package com.water0.hydration.data.ml

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * On-device tomorrow-intake forecast: our own tiny temporal-conv net
 * (7 daily totals in, one number out), trained in ml_training/
 * (train_tcn.py). Fixed /4000 input scale — no scaler file to ship.
 *
 * Advisor only, never driver: anything can fail here (missing asset,
 * runtime mismatch) and every caller must treat null as "no opinion".
 * Rules decide; this whispers.
 */
class OnDeviceForecast(appContext: Context) {

    private val assets = appContext.applicationContext.assets

    /**
     * @param last7TotalsMl previous 7 *complete* days, oldest first.
     * @return predicted tomorrow total, or null when unavailable.
     */
    fun predictTomorrowMl(last7TotalsMl: List<Int>): Int? {
        if (last7TotalsMl.size != WINDOW_DAYS) return null
        return try {
            val interpreter = interpreter() ?: return null
            val input = ByteBuffer
                .allocateDirect(WINDOW_DAYS * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            last7TotalsMl.forEach { ml -> input.put(ml / SCALE) }
            input.rewind()
            val output = Array(1) { FloatArray(1) }
            interpreter.run(input, output)
            (output[0][0] * SCALE).toInt().coerceIn(0, 8000)
        } catch (_: Exception) {
            null
        }
    }

    private var cached: org.tensorflow.lite.Interpreter? = null
    private var broken = false

    private fun interpreter(): org.tensorflow.lite.Interpreter? {
        cached?.let { return it }
        if (broken) return null
        return try {
            val bytes = assets.open(MODEL_ASSET).use { it.readBytes() }
            val buffer = ByteBuffer
                .allocateDirect(bytes.size)
                .order(ByteOrder.nativeOrder())
            buffer.put(bytes)
            buffer.rewind()
            org.tensorflow.lite.Interpreter(buffer).also { cached = it }
        } catch (_: Exception) {
            broken = true
            null
        }
    }

    companion object {
        const val MODEL_ASSET = "sequence_tcn.tflite"
        const val WINDOW_DAYS = 7
        const val SCALE = 4000f
    }
}
