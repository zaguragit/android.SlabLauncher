package io.posidon.android.slablauncher.data.items

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.view.View

class ContactItem(
    override var label: String,
    val lookupKey: String,
    val phone: String,
    val id: Int,
    val isStarred: Boolean,
    val photoId: String?,
) : LauncherItem {

    override fun open(context: Context, view: View?) {
        val viewContact = Intent(Intent.ACTION_VIEW)
        viewContact.data = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
        viewContact.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        viewContact.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        context.startActivity(viewContact)
    }

    override fun toString() = "$id/$lookupKey"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ContactItem

        if (id == other.id) return true
        if (lookupKey != other.lookupKey) return false
        if (phone != other.phone) return false
        if (label != other.label) return false

        return true
    }

    override fun hashCode() = id

    companion object {
        fun tryParse(string: String, contactList: Collection<ContactItem>): ContactItem? =
            try { parse(string, contactList) }
            catch (e: Exception) { null }.also {
                it ?: Log.v("LauncherItem parsing", "\"$string\" !is ContactItem")
            }

        fun parse(string: String, contactList: Collection<ContactItem>): ContactItem? {
            val (idStr, lookupKey) = string.split('/')
            val id = idStr.toInt()
            return contactList.find {
                it.id == id && it.lookupKey == lookupKey
            }
        }
    }
}