package com.scimsoft.tap2share.util

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.scimsoft.tap2share.model.ShareItem
import com.scimsoft.tap2share.model.ShareType

object ContentUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "unknown"

        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(
                uri, null, null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }

        if (name == "unknown") {
            name = uri.lastPathSegment ?: "unknown"
        }

        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L

        if (uri.scheme == "content") {
            val cursor: Cursor? = context.contentResolver.query(
                uri, null, null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.SIZE)
                    if (index >= 0) {
                        size = it.getLong(index)
                    }
                }
            }
        }

        return size
    }

    fun getMimeType(context: Context, uri: Uri): String {
        return context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(
                    MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                )
            ?: "*/*"
    }

    fun determineShareType(mimeType: String): ShareType {
        return when {
            mimeType.startsWith("image/") -> ShareType.IMAGE
            mimeType.startsWith("video/") -> ShareType.VIDEO
            mimeType == "text/x-vcard" || mimeType == "text/vcard" -> ShareType.CONTACT
            mimeType.startsWith("text/") -> ShareType.TEXT
            else -> ShareType.FILE
        }
    }

    fun createShareItem(context: Context, uri: Uri, mimeType: String? = null): ShareItem {
        val actualMimeType = mimeType ?: getMimeType(context, uri)
        val fileName = getFileName(context, uri)
        val fileSize = getFileSize(context, uri)
        val type = determineShareType(actualMimeType)

        return ShareItem(
            type = type,
            uri = uri,
            mimeType = actualMimeType,
            fileName = fileName,
            fileSize = fileSize
        )
    }

    fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
