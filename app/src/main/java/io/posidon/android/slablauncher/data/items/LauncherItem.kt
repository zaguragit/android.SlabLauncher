package io.posidon.android.slablauncher.data.items

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import io.posidon.android.slablauncher.providers.item.ContactLoader
import java.util.*

sealed interface LauncherItem {

    val label: String

    /**
     * What to do when the item is clicked
     * [view] The view that was clicked
     */
    fun open(context: Context, view: View?)

    /**
     * Text representation of the item, used to save it
     */
    override fun toString(): String

    companion object {
        fun tryParse(
            string: String,
            appsByName: HashMap<String, MutableList<App>>,
            context: Context
        ): LauncherItem? = App.tryParse(string, appsByName)
            ?: ContactItem.tryParse(string, ContactLoader.load(context))
    }
}

fun LauncherItem.showProperties(view: View) {
    if (this is App) {
        view.context.startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .setData(Uri.parse("package:$packageName")))
    }
}