package io.posidon.android.slablauncher.util.storage

import android.content.Context
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

object PrivateStorage {

    inline fun <T> read(context: Context, path: String, block: (ObjectInputStream) -> T): T? {
        return try { ObjectInputStream(context.openFileInput(path)).use(block) }
        catch (e: IOException) { null }
    }

    fun write(context: Context, path: String, block: (ObjectOutputStream) -> Unit) {
        try { ObjectOutputStream(context.openFileOutput(path, Context.MODE_PRIVATE)).use(block) }
        catch (e: IOException) { e.printStackTrace() }
    }
}