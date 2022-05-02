package io.posidon.android.slablauncher.providers.item

import android.content.Context
import android.provider.ContactsContract
import io.posidon.android.slablauncher.data.items.ContactItem

object ContactLoader {

    fun load(context: Context): Collection<ContactItem> {
        val cur = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.STARRED,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                ContactsContract.Contacts.PHOTO_ID,
                ContactsContract.Contacts._ID), null, null, null)

        val contactMap = HashMap<String, ContactItem>()

        if (cur != null) {
            val lookupIndex = cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY)
            val displayNameIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val starredIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
            val photoIdIndex = cur.getColumnIndex(ContactsContract.Contacts.PHOTO_ID)
            val contactIdIndex = cur.getColumnIndex(ContactsContract.Contacts._ID)

            if (cur.count != 0) {
                while (cur.moveToNext()) {
                    val starred = cur.getInt(starredIndex) != 0
                    val lookupKey = cur.getString(lookupIndex)
                    val name = cur.getString(displayNameIndex)
                    if (name.isNullOrBlank()) continue
                    val contactId = cur.getInt(contactIdIndex)
                    val phone = cur.getString(numberIndex) ?: ""
                    val photoId = cur.getString(photoIdIndex)

                    val contact = ContactItem(name, lookupKey, phone, contactId, starred, photoId)

                    if (!contactMap.containsKey(lookupKey)) {
                        contactMap[lookupKey] = contact
                    }
                }
            }
            cur.close()
        }

        val nicknameCur = context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Nickname.NAME, ContactsContract.Data.LOOKUP_KEY),
            ContactsContract.Data.MIMETYPE + "= ?",
            arrayOf(ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE), null)

        if (nicknameCur != null) {
            if (nicknameCur.count != 0) {
                val lookupKeyIndex = nicknameCur.getColumnIndex(ContactsContract.Data.LOOKUP_KEY)
                val nickNameIndex = nicknameCur.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME)
                while (nicknameCur.moveToNext()) {
                    val lookupKey = nicknameCur.getString(lookupKeyIndex)
                    val nickname = nicknameCur.getString(nickNameIndex)
                    if (nickname != null && lookupKey != null && contactMap.containsKey(lookupKey)) {
                        contactMap[lookupKey]!!.label = nickname
                    }
                }
            }
            nicknameCur.close()
        }

        return contactMap.values
    }
}