package com.tapshare.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tapshare.app.model.*
import com.tapshare.app.nfc.NfcManager
import com.tapshare.app.transfer.FileTransferService
import com.tapshare.app.transfer.WifiDirectManager
import com.tapshare.app.util.ContentUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val nfcManager = NfcManager()
    val wifiDirectManager = WifiDirectManager(application)

    private val _currentScreen = MutableStateFlow(Screen.HOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _shareItem = MutableStateFlow<ShareItem?>(null)
    val shareItem: StateFlow<ShareItem?> = _shareItem.asStateFlow()

    private val _transferProgress = MutableStateFlow(TransferProgress())
    val transferProgress: StateFlow<TransferProgress> = _transferProgress.asStateFlow()

    private var transferService: FileTransferService? = null
    private var sessionId = UUID.randomUUID().toString()

    init {
        wifiDirectManager.initialize()
    }

    fun bindTransferService(service: FileTransferService) {
        transferService = service
        viewModelScope.let {
            // Observe service progress
        }
    }

    fun setShareText(text: String) {
        _shareItem.value = ShareItem(
            type = if (isUrl(text)) ShareType.URL else ShareType.TEXT,
            text = text
        )
        _currentScreen.value = Screen.SENDING
        prepareToSend()
    }

    fun setShareFile(uri: Uri) {
        val context = getApplication<Application>()
        _shareItem.value = ContentUtils.createShareItem(context, uri)
        _currentScreen.value = Screen.SENDING
        prepareToSend()
    }

    fun setShareImage(uri: Uri) {
        val context = getApplication<Application>()
        _shareItem.value = ContentUtils.createShareItem(context, uri, "image/*")
        _currentScreen.value = Screen.SENDING
        prepareToSend()
    }

    fun setShareItems(uris: List<Uri>) {
        _shareItem.value = ShareItem(
            type = ShareType.MULTIPLE_FILES,
            uris = uris
        )
        _currentScreen.value = Screen.SENDING
        prepareToSend()
    }

    fun startReceiving() {
        _currentScreen.value = Screen.RECEIVING
        _transferProgress.value = TransferProgress(state = TransferState.WAITING_FOR_TAP)

        transferService?.startServer(FileTransferService.DEFAULT_PORT) { port ->
            // Server ready on port
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun navigateBack() {
        _currentScreen.value = Screen.HOME
        resetTransfer()
    }

    fun cancelTransfer() {
        wifiDirectManager.disconnect()
        resetTransfer()
        _currentScreen.value = Screen.HOME
    }

    fun handleNfcIntent(transferInfo: TransferInfo) {
        when (_currentScreen.value) {
            Screen.SENDING -> {
                // Sender tapped: initiate the transfer to the receiver
                _transferProgress.value = TransferProgress(state = TransferState.CONNECTING)
                val item = _shareItem.value ?: return
                when (item.type) {
                    ShareType.TEXT, ShareType.URL -> {
                        transferService?.sendText(
                            transferInfo.ipAddress,
                            transferInfo.port,
                            item.text ?: ""
                        )
                    }
                    else -> {
                        item.uri?.let { uri ->
                            transferService?.sendFile(
                                transferInfo.ipAddress,
                                transferInfo.port,
                                uri,
                                item.fileName ?: "file",
                                item.mimeType
                            )
                        }
                    }
                }
            }
            else -> {
                // Auto-launch into receive mode from any other screen
                _currentScreen.value = Screen.RECEIVING
                _transferProgress.value = TransferProgress(state = TransferState.CONNECTING)
                transferService?.startServer(FileTransferService.DEFAULT_PORT) { _ -> }
            }
        }
    }

    fun getTransferInfo(): TransferInfo {
        val ipAddress = ContentUtils.getLocalIpAddress() ?: "0.0.0.0"
        return TransferInfo(
            sessionId = sessionId,
            senderName = android.os.Build.MODEL,
            itemCount = if (_shareItem.value?.type == ShareType.MULTIPLE_FILES) {
                _shareItem.value?.uris?.size ?: 1
            } else 1,
            totalSize = _shareItem.value?.fileSize ?: 0L,
            ipAddress = ipAddress,
            port = FileTransferService.DEFAULT_PORT
        )
    }

    private fun prepareToSend() {
        sessionId = UUID.randomUUID().toString()
        _transferProgress.value = TransferProgress(state = TransferState.WAITING_FOR_TAP)
        wifiDirectManager.createGroup()
    }

    private fun resetTransfer() {
        _transferProgress.value = TransferProgress()
        _shareItem.value = null
        transferService?.resetProgress()
    }

    private fun isUrl(text: String): Boolean {
        return text.startsWith("http://") ||
                text.startsWith("https://") ||
                text.startsWith("www.")
    }

    override fun onCleared() {
        super.onCleared()
        wifiDirectManager.cleanup()
    }
}

enum class Screen {
    HOME,
    SHARE_TEXT,
    SENDING,
    RECEIVING
}
