package com.tapshare.app.model

import android.net.Uri

enum class ShareType {
    TEXT,
    URL,
    IMAGE,
    VIDEO,
    FILE,
    CONTACT,
    MULTIPLE_FILES
}

data class ShareItem(
    val type: ShareType,
    val text: String? = null,
    val uri: Uri? = null,
    val uris: List<Uri>? = null,
    val mimeType: String = "*/*",
    val fileName: String? = null,
    val fileSize: Long = 0L
) {
    val displayName: String
        get() = when (type) {
            ShareType.TEXT -> text?.take(50) ?: "Text"
            ShareType.URL -> text ?: "URL"
            ShareType.IMAGE -> fileName ?: "Image"
            ShareType.VIDEO -> fileName ?: "Video"
            ShareType.FILE -> fileName ?: "File"
            ShareType.CONTACT -> fileName ?: "Contact"
            ShareType.MULTIPLE_FILES -> "${uris?.size ?: 0} files"
        }
}

data class TransferInfo(
    val sessionId: String,
    val senderName: String,
    val itemCount: Int,
    val totalSize: Long,
    val ipAddress: String,
    val port: Int
)

enum class TransferState {
    IDLE,
    WAITING_FOR_TAP,
    CONNECTING,
    TRANSFERRING,
    COMPLETED,
    FAILED
}

data class TransferProgress(
    val state: TransferState = TransferState.IDLE,
    val progress: Float = 0f,
    val currentFile: String = "",
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
)
