package io.posidon.android.slablauncher.util.blur

import android.graphics.Bitmap
import io.posidon.android.slablauncher.util.FastRandom

object NoiseBlur {
    fun blur(source: Bitmap, radius: Float): Bitmap {
        val width = source.width
        val height = source.height
        val origPixels = IntArray(width * height)
        val newPixels = IntArray(width * height)
        source.getPixels(origPixels, 0, width, 0, 0, width, height)
        val d = (Int.MAX_VALUE / 2).toFloat()
        val random = FastRandom((height.toLong() shl 4) * 51 or (origPixels[0].toLong() shl 17))
        for (y in 0 until height) {
            for (x in 0 until width) {
                val long = random.nextLong()
                var ox = (long shr 32).toInt() / d - 1
                var oy = long.toInt() / d - 1
                val l = ox * ox + oy * oy
                ox *= radius / l
                oy *= radius / l
                val nx = run {
                    val a = (x + ox.toInt())
                    when {
                        a > width - 1 -> width - 1
                        a < 0 -> 0
                        else -> a
                    }
                }
                val ny = run {
                    val a = (y + oy.toInt())
                    when {
                        a > height - 1 -> height - 1
                        a < 0 -> 0
                        else -> a
                    }
                }
                newPixels[y * width + x] = origPixels[ny * width + nx]
            }
        }
        val bmOut = Bitmap.createBitmap(width, height, source.config)
        bmOut.setPixels(newPixels, 0, width, 0, 0, width, height)
        return bmOut
    }
}