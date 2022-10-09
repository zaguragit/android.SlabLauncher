package io.posidon.android.slablauncher.util.blur

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import io.posidon.android.conveniencelib.Device
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toFloatPixels
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.slablauncher.util.drawable.NonDrawable
import kotlin.concurrent.thread
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class AcrylicBlur private constructor(
    resources: Resources,
    val fullBlur: Bitmap,
    val smoothBlur: Bitmap,
    val partialBlurMedium: Bitmap,
    val insaneBlur: Bitmap,
) {

    var fullBlurDrawable: Drawable = BitmapDrawable(resources, fullBlur)
        private set
    var smoothBlurDrawable: Drawable = BitmapDrawable(resources, smoothBlur)
        private set
    var partialBlurMediumDrawable: Drawable = BitmapDrawable(resources, partialBlurMedium)
        private set
    var insaneBlurDrawable: Drawable = BitmapDrawable(resources, insaneBlur)
        private set

    fun recycle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (fullBlurDrawable as BitmapDrawable).bitmap = null
            (smoothBlurDrawable as BitmapDrawable).bitmap = null
            (partialBlurMediumDrawable as BitmapDrawable).bitmap = null
            (insaneBlurDrawable as BitmapDrawable).bitmap = null
            fullBlurDrawable = NonDrawable()
            smoothBlurDrawable = NonDrawable()
            partialBlurMediumDrawable = NonDrawable()
            insaneBlurDrawable = NonDrawable()
        }
        fullBlur.recycle()
        smoothBlur.recycle()
        partialBlurMedium.recycle()
        insaneBlur.recycle()
    }

    companion object {
        fun blurWallpaper(context: Context, drawable: Drawable): AcrylicBlur {
            val h = Device.screenHeight(context).coerceAtMost(2040)
            val w = h * drawable.intrinsicWidth / drawable.intrinsicHeight
            val b = drawable.toBitmap(w / 8, h / 8)
            val sb = drawable.toBitmap(48, 48 * h / w)
            val insaneBlur = fastBlur(sb, 8)
            val smoothBlur = fastBlur(b, 1.5.dp.toPixels(context))
            val partialBlurMedium = fastBlur(b, 1.dp.toPixels(context))
            val nb = Bitmap.createScaledBitmap(smoothBlur, w, h, false)
            val fullBlur = NoiseBlur.blur(nb, 18.dp.toFloatPixels(context))
            return AcrylicBlur(context.resources, fullBlur, smoothBlur, partialBlurMedium, insaneBlur)
        }

        inline fun blurWallpaper(
            context: Context,
            drawable: Drawable,
            crossinline onEnd: (AcrylicBlur) -> Unit
        ): Thread = thread(isDaemon = true) {
            onEnd(blurWallpaper(context, drawable))
        }

        fun fastBlur(bitmap: Bitmap, radius: Int): Bitmap {
            if (radius < 1) {
                return bitmap
            }
            val initWidth = bitmap.width
            val initHeight = bitmap.height
            val d = radius.toFloat()
            val w = (initWidth / d).roundToInt()
            val h = (initHeight / d).roundToInt()
            var bitmap = Bitmap.createScaledBitmap(bitmap, w, h, false)
            bitmap = bitmap.copy(bitmap.config, true)
            val pix = IntArray(w * h)
            bitmap.getPixels(pix, 0, w, 0, 0, w, h)
            val wm = w - 1
            val hm = h - 1
            val wh = w * h
            val div = radius + radius + 1
            val r = IntArray(wh)
            val g = IntArray(wh)
            val b = IntArray(wh)
            var rsum: Int
            var gsum: Int
            var bsum: Int
            var x: Int
            var y: Int
            var i: Int
            var p: Int
            var yp: Int
            var yi: Int
            val vmin = IntArray(max(w, h))
            var divsum = div + 1 shr 1
            divsum *= divsum
            val dv = IntArray(256 * divsum)
            i = 0
            while (i < 256 * divsum) {
                dv[i] = i / divsum
                i++
            }
            yi = 0
            var yw: Int = yi
            val stack = Array(div) { IntArray(3) }
            var stackpointer: Int
            var stackstart: Int
            var sir: IntArray
            var rbs: Int
            val r1 = radius + 1
            var routsum: Int
            var goutsum: Int
            var boutsum: Int
            var rinsum: Int
            var ginsum: Int
            var binsum: Int
            y = 0
            while (y < h) {
                bsum = 0
                gsum = bsum
                rsum = gsum
                boutsum = rsum
                goutsum = boutsum
                routsum = goutsum
                binsum = routsum
                ginsum = binsum
                rinsum = ginsum
                i = -radius
                while (i <= radius) {
                    p = pix[yi + min(wm, max(i, 0))]
                    sir = stack[i + radius]
                    sir[0] = p and 0xff0000 shr 16
                    sir[1] = p and 0x00ff00 shr 8
                    sir[2] = p and 0x0000ff
                    rbs = r1 - abs(i)
                    rsum += sir[0] * rbs
                    gsum += sir[1] * rbs
                    bsum += sir[2] * rbs
                    if (i > 0) {
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }
                    i++
                }
                stackpointer = radius
                x = 0
                while (x < w) {
                    r[yi] = dv[rsum]
                    g[yi] = dv[gsum]
                    b[yi] = dv[bsum]
                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum
                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]
                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]
                    if (y == 0) vmin[x] = min(x + radius + 1, wm)
                    p = pix[yw + vmin[x]]
                    sir[0] = p and 0xff0000 shr 16
                    sir[1] = p and 0x00ff00 shr 8
                    sir[2] = p and 0x0000ff
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum
                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer % div]
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]
                    yi++
                    x++
                }
                yw += w
                y++
            }
            x = 0
            while (x < w) {
                bsum = 0
                gsum = bsum
                rsum = gsum
                boutsum = rsum
                goutsum = boutsum
                routsum = goutsum
                binsum = routsum
                ginsum = binsum
                rinsum = ginsum
                yp = -radius * w
                i = -radius
                while (i <= radius) {
                    yi = max(0, yp) + x
                    sir = stack[i + radius]
                    sir[0] = r[yi]
                    sir[1] = g[yi]
                    sir[2] = b[yi]
                    rbs = r1 - abs(i)
                    rsum += r[yi] * rbs
                    gsum += g[yi] * rbs
                    bsum += b[yi] * rbs
                    if (i > 0) {
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }
                    if (i < hm) yp += w
                    i++
                }
                yi = x
                stackpointer = radius
                y = 0
                while (y < h) {
                    // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                    pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum
                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]
                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]
                    if (x == 0) vmin[y] = min(y + r1, hm) * w
                    p = x + vmin[y]
                    sir[0] = r[p]
                    sir[1] = g[p]
                    sir[2] = b[p]
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum
                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer]
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]
                    yi += w
                    y++
                }
                x++
            }
            bitmap.setPixels(pix, 0, w, 0, 0, w, h)
            return Bitmap.createScaledBitmap(bitmap, initWidth, initHeight, true)
        }
    }
}