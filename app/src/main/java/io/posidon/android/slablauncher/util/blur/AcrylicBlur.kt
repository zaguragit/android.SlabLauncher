package io.posidon.android.slablauncher.util.blur

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import posidon.android.conveniencelib.Device
import posidon.android.conveniencelib.Graphics
import posidon.android.conveniencelib.dp
import kotlin.concurrent.thread

class AcrylicBlur private constructor(
    resources: Resources,
    val fullBlur: Bitmap,
    val smoothBlur: Bitmap,
    val partialBlurMedium: Bitmap,
    val partialBlurSmall: Bitmap,
    val insaneBlur: Bitmap,
) {

    val fullBlurDrawable: Drawable = BitmapDrawable(resources, fullBlur)
    val smoothBlurDrawable: Drawable = BitmapDrawable(resources, smoothBlur)
    val partialBlurMediumDrawable: Drawable = BitmapDrawable(resources, partialBlurMedium)
    val partialBlurSmallDrawable: Drawable = BitmapDrawable(resources, partialBlurSmall)
    val insaneBlurDrawable: Drawable = BitmapDrawable(resources, insaneBlur)

    fun recycle() {
        fullBlur.recycle()
        smoothBlur.recycle()
        partialBlurMedium.recycle()
        partialBlurSmall.recycle()
        insaneBlur.recycle()
    }

    companion object {
        fun blurWallpaper(context: Context, drawable: Drawable): AcrylicBlur {
            val w = Device.screenHeight(context) * drawable.intrinsicWidth / drawable.intrinsicHeight
            val h = Device.screenHeight(context)
            val b = drawable.toBitmap(w / 12, h / 12)
            val sb = drawable.toBitmap(48, h * 48 / w)
            val insaneBlur = Graphics.fastBlur(sb, 8)
            val smoothBlur = Graphics.fastBlur(b, context.dp(1f).toInt())
            val partialBlurMedium = Graphics.fastBlur(b, context.dp(.6f).toInt())
            val partialBlurSmall = Graphics.fastBlur(b, context.dp(.3f).toInt())
            val nb = Bitmap.createScaledBitmap(smoothBlur, w, h, false)
            val fullBlur = NoiseBlur.blur(nb, context.dp(18f))
            return AcrylicBlur(context.resources, fullBlur, smoothBlur, partialBlurMedium, partialBlurSmall, insaneBlur)
        }

        inline fun blurWallpaper(
            context: Context,
            drawable: Drawable,
            crossinline onEnd: (AcrylicBlur) -> Unit
        ): Thread = thread(isDaemon = true) {
            onEnd(blurWallpaper(context, drawable))
        }
    }
}