package io.posidon.android.slablauncher.providers.item

import android.content.ContentUris
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.*
import android.graphics.drawable.*
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.graphics.*
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import io.posidon.android.conveniencelib.drawable.MaskedDrawable
import io.posidon.android.conveniencelib.drawable.clone
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.launcherutil.Launcher
import io.posidon.android.launcherutil.isUserRunning
import io.posidon.android.launcherutil.loader.AppIconLoader
import io.posidon.android.launcherutil.loader.IconData
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.ContactItem
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.items.ShortcutItem
import io.posidon.android.slablauncher.util.drawable.ContactDrawable
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable
import io.posidon.android.slablauncher.util.drawable.NonDrawable
import io.posidon.android.slablauncher.util.storage.Settings
import java.io.FileNotFoundException
import java.lang.ref.WeakReference
import java.util.concurrent.Future
import kotlin.math.min
import kotlin.random.Random

class GraphicsLoader {

    class Extra(
        val tile: Drawable,
        var color: Int,
    )

    inline fun load(
        context: Context,
        item: LauncherItem,
        crossinline onLoaded: (IconData<Extra>) -> Unit,
    ): Future<Unit> = loader.submit {
        onLoaded(load(context, item))
    }

    inline fun load(
        context: Context,
        item: LauncherItem,
    ): IconData<Extra> {
        return when (item) {
            is App -> load(context, item)
            is ContactItem -> load(context, item)
            is ShortcutItem -> load(context, item)
        }
    }

    inline fun load(
        context: Context,
        app: App,
    ): IconData<Extra> = loader.load(context, app.packageName, app.name, app.userHandle)

    fun load(
        context: Context,
        shortcut: ShortcutItem,
    ): IconData<Extra> {
        val d = context.getSystemService(LauncherApps::class.java).getShortcutIconDrawable(
            shortcut.shortcutInfo,
            context.resources.displayMetrics.densityDpi
        )?.let { modifyIcon(it, forceMask = true).first } ?: NonDrawable()
        return IconData(d, Extra(d, 0))
    }

    fun load(
        context: Context,
        contact: ContactItem,
    ): IconData<Extra> {
        val tmpLab = DoubleArray(3)
        val textP = Paint().apply {
            color = 0xffffffff.toInt()
            typeface = context.resources.getFont(R.font.rubik_medium_caps)
            textAlign = Paint.Align.CENTER
            textSize = 64f
            isAntiAlias = true
            isSubpixelText = true
        }

        val iconUri: Uri? = if (contact.photoId != null) {
            ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI, contact.photoId.toLong())
        } else null

        val pic = if (iconUri == null) genProfilePic(
            contact.label,
            tmpLab,
            textP
        ) else try {
            val inputStream = context.contentResolver.openInputStream(iconUri)
            Drawable.createFromStream(inputStream, iconUri.toString())!!
        } catch (e: FileNotFoundException) {
            genProfilePic(contact.label, tmpLab, textP)
        }
        val w = pic.intrinsicWidth
        val h = pic.intrinsicHeight
        pic.setBounds(0, 0, w, h)
        val i = IconData(
            MaskedDrawable(
                pic,
                makeCirclePath(w, h)
            ),
            Extra(
                pic,
                0,
            )
        )
        contactIcons[contact] = WeakReference(i)
        return i
    }

    fun setupNewAppIconLoader(context: Context, settings: Settings) {
        loader = createAppIconLoader(context, settings)
    }

    lateinit var loader: AppIconLoader<Extra>
        private set

    private val contactIcons = HashMap<ContactItem, WeakReference<IconData<Extra>>>()

    private fun createAppIconLoader(context: Context, settings: Settings): AppIconLoader<Extra> {
        return Launcher.iconLoader(
            context,
            size = 108.dp.toPixels(context),
            density = context.resources.configuration.densityDpi,
            packPackages = settings.getStrings("icon_packs") ?: emptyArray(),
            iconModifier = { _, _, profile, icon, resizableBackground ->
                val (ic, extra) = modifyIcon(icon)
                val isUserRunning = isUserRunning(profile)
                if (!isUserRunning) {
                    extra.color = run {
                        val a = (extra.color.luminance * 255).toInt()
                        Color.rgb(a, a, a)
                    }
                    ic.makeGrayscale()
                    extra.tile.makeGrayscale()
                }
                ic to extra
            }
        )
    }

    private fun modifyIcon(icon: Drawable?, forceMask: Boolean = false): Pair<Drawable, Extra> {
        var color = 0
        val image: LayerDrawable
        val finalIcon: Drawable

        when {
            icon is AdaptiveIconDrawable &&
            icon.background != null -> {
                val iconForeground: Drawable? = icon.foreground
                val background: Drawable
                when (val b = icon.background) {
                    is ColorDrawable -> {
                        color = ensureNotPlainWhite(b.color, icon)
                        background = FastColorDrawable(b.color)
                    }
                    is ShapeDrawable -> {
                        color = ensureNotPlainWhite(b.paint.color, icon)
                        background = FastColorDrawable(b.paint.color)
                    }
                    is GradientDrawable -> {
                        val bitmap = b.toBitmap(8, 8)
                        color = b.color?.defaultColor ?: Palette.from(bitmap).generate().getDominantColor(0)
                        bitmap.recycle()
                        background = icon.background
                    }
                    else -> {
                        val bitmap = b.toBitmap(24, 24)
                        color = Palette.from(bitmap).generate().getDominantColor(0)
                        if (b !is BitmapDrawable || b.bitmap != bitmap) bitmap.recycle()
                        background = icon.background
                    }
                }
                image = LayerDrawable(arrayOf(
                    background,
                    iconForeground,
                ))
                val bg = (background.clone() ?: background).mutate()
                val fg = (iconForeground?.clone() ?: iconForeground)?.mutate()
                val maskable = LayerDrawable(arrayOf(
                    bg,
                    fg
                )).apply {
                    val i = -(intrinsicWidth / 8f).toInt()
                    setLayerInset(0, i, i, i, i)
                    setLayerInset(1, i, i, i, i)
                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                }
                finalIcon = MaskedDrawable(
                    maskable,
                    makeCirclePath(maskable.intrinsicWidth, maskable.intrinsicHeight),
                )
            }
            else -> {
                if (icon != null) {
                    val bitmap = icon.toBitmap(32, 32)
                    val palette = Palette.from(bitmap).generate()
                    color = palette.getDominantColor(0)
                    if (color.red == color.blue && color.blue == color.green && color.green > 0xd0) {
                        color = 0
                    }
                    if (icon !is BitmapDrawable || icon.bitmap != bitmap) bitmap.recycle()
                }
                image = LayerDrawable(arrayOf(
                    FastColorDrawable(color),
                    icon
                )).apply {
                    val i = if (forceMask) 0 else intrinsicWidth / 4
                    setLayerInset(0, i, i, i, i)
                    setLayerInset(1, i, i, i, i)
                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                }
                finalIcon = if (forceMask && icon != null) MaskedDrawable(
                    image,
                    makeCirclePath(image.intrinsicWidth, image.intrinsicHeight),
                ) else (icon?.clone() ?: icon)?.mutate() ?: NonDrawable()
            }
        }
        return finalIcon to Extra(image, color)
    }

    private fun ensureNotPlainWhite(
        color: Int,
        icon: AdaptiveIconDrawable
    ): Int {
        if (color == 0xffffffff.toInt()) {
            val fg = icon.foreground
            val bitmap = fg.toBitmap(24, 24)
            val c = Palette.from(bitmap).generate()
                .getDominantColor(color)
            if (fg !is BitmapDrawable || fg.bitmap != bitmap) bitmap.recycle()
            val tmpLab = DoubleArray(3)
            ColorUtils.colorToLAB(c, tmpLab)
            tmpLab[0] = (tmpLab[0] * 1.5).coerceAtLeast(70.0)
            return ColorUtils.LABToColor(tmpLab[0], tmpLab[1], tmpLab[2])
        }
        return color
    }

    private fun genProfilePic(name: String, tmpLab: DoubleArray, paint: Paint): Drawable {
        val realName = name.trim { !it.isLetterOrDigit() }.uppercase().padEnd(4, '$')
        val key = (realName[0].code shl (16 * 3)).toLong() + (realName[2].code shl (16 * 2)).toLong() + (realName[1].code shl 16).toLong() + realName.last().code
        val random = Random(key)
        val color = run {
            val base = Color.HSVToColor(floatArrayOf(random.nextFloat() * 360f, 1f, 1f))
            ColorUtils.colorToLAB(base, tmpLab)
            tmpLab[1] *= 0.8
            tmpLab[2] *= 0.8
            ColorUtils.LABToColor(
                50.0,
                tmpLab[1],
                tmpLab[2],
            )
        }
        val lightColor = run {
            ColorUtils.LABToColor(
                50.0,
                (tmpLab[1] + random.nextFloat().let { it * 2 - 1 } * 60.0) * 1.2,
                (tmpLab[2] + random.nextFloat().let { it * 2 - 1 } * 60.0) * 1.2,
            )
        }
        return ContactDrawable(
            color,
            lightColor,
            realName[0],
            paint
        )
    }

    private fun makeCirclePath(w: Int, h: Int) = Path().apply {
        fillType = Path.FillType.INVERSE_EVEN_ODD
        addCircle(w / 2f, h / 2f, min(w, h) / 2f - 2, Path.Direction.CCW)
    }

    companion object {
        inline fun Drawable.makeGrayscale(): Drawable {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                setSaturation(0f)
            })
            return this
        }
    }
}