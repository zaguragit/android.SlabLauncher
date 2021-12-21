package io.posidon.android.slablauncher.util.blur

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import io.posidon.android.slablauncher.util.drawable.NonDrawable
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

    var fullBlurDrawable: Drawable = BitmapDrawable(resources, fullBlur)
        private set
    var smoothBlurDrawable: Drawable = BitmapDrawable(resources, smoothBlur)
        private set
    var partialBlurMediumDrawable: Drawable = BitmapDrawable(resources, partialBlurMedium)
        private set
    var partialBlurSmallDrawable: Drawable = BitmapDrawable(resources, partialBlurSmall)
        private set
    var insaneBlurDrawable: Drawable = BitmapDrawable(resources, insaneBlur)
        private set

    fun recycle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (fullBlurDrawable as BitmapDrawable).bitmap = null
            (smoothBlurDrawable as BitmapDrawable).bitmap = null
            (partialBlurMediumDrawable as BitmapDrawable).bitmap = null
            (partialBlurSmallDrawable as BitmapDrawable).bitmap = null
            (insaneBlurDrawable as BitmapDrawable).bitmap = null
            fullBlurDrawable = NonDrawable()
            smoothBlurDrawable = NonDrawable()
            partialBlurMediumDrawable = NonDrawable()
            partialBlurSmallDrawable = NonDrawable()
            insaneBlurDrawable = NonDrawable()
        }
        fullBlur.recycle()
        smoothBlur.recycle()
        partialBlurMedium.recycle()
        partialBlurSmall.recycle()
        insaneBlur.recycle()
    }

    companion object {
        fun blurWallpaper(context: Context, drawable: Drawable): AcrylicBlur {
            val h = Device.screenHeight(context).coerceAtMost(2040)
            val w = h * drawable.intrinsicWidth / drawable.intrinsicHeight
            val b = drawable.toBitmap(w / 12, h / 12)
            val sb = drawable.toBitmap(48, 48 * h / w)
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