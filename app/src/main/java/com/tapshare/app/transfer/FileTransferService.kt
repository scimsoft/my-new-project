package com.tapshare.app.transfer

import android.app.Notification
import android.app.Service
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tapshare.app.R
import com.tapshare.app.TapShareApp
import com.tapshare.app.model.TransferProgress
import com.tapshare.app.model.TransferState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class FileTransferService : Service() {

    private val binder = TransferBinder()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _progress = MutableStateFlow(TransferProgress())
    val progress: StateFlow<TransferProgress> = _progress.asStateFlow()

    private var serverSocket: ServerSocket? = null

    inner class TransferBinder : Binder() {
        fun getService(): FileTransferService = this@FileTransferService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification("TapShare ready"))
    }

    override fun onDestroy() {
        scope.cancel()
        serverSocket?.close()
        super.onDestroy()
    }

    fun startServer(port: Int, onReady: (Int) -> Unit) {
        scope.launch {
            try {
                serverSocket = ServerSocket(port)
                val actualPort = serverSocket!!.localPort
                withContext(Dispatchers.Main) {
                    onReady(actualPort)
                }

                _progress.value = TransferProgress(state = TransferState.WAITING_FOR_TAP)

                val clientSocket = serverSocket!!.accept()
                handleIncomingConnection(clientSocket)
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
                _progress.value = TransferProgress(
                    state = TransferState.FAILED,
                    errorMessage = e.message
                )
            }
        }
    }

    fun sendFile(
        host: String,
        port: Int,
        uri: Uri,
        fileName: String,
        mimeType: String
    ) {
        scope.launch {
            try {
                _progress.value = TransferProgress(state = TransferState.CONNECTING)

                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), CONNECTION_TIMEOUT)

                _progress.value = TransferProgress(
                    state = TransferState.TRANSFERRING,
                    currentFile = fileName
                )

                val outputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                val inputStream = contentResolver.openInputStream(uri)
                    ?: throw IOException("Cannot open file: $uri")

                val fileBytes = inputStream.readBytes()
                inputStream.close()

                outputStream.writeUTF(fileName)
                outputStream.writeUTF(mimeType)
                outputStream.writeLong(fileBytes.size.toLong())

                val buffer = ByteArray(BUFFER_SIZE)
                val byteStream = ByteArrayInputStream(fileBytes)
                var bytesRead: Int
                var totalSent = 0L
                val totalSize = fileBytes.size.toLong()

                while (byteStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalSent += bytesRead
                    _progress.value = TransferProgress(
                        state = TransferState.TRANSFERRING,
                        progress = totalSent.toFloat() / totalSize,
                        currentFile = fileName,
                        bytesTransferred = totalSent,
                        totalBytes = totalSize
                    )
                    updateNotification("Sending: ${(totalSent * 100 / totalSize)}%")
                }

                outputStream.flush()
                socket.close()

                _progress.value = TransferProgress(state = TransferState.COMPLETED)
                updateNotification("Transfer complete")

            } catch (e: Exception) {
                Log.e(TAG, "Send error", e)
                _progress.value = TransferProgress(
                    state = TransferState.FAILED,
                    errorMessage = e.message
                )
            }
        }
    }

    fun sendText(host: String, port: Int, text: String) {
        scope.launch {
            try {
                _progress.value = TransferProgress(state = TransferState.CONNECTING)

                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), CONNECTION_TIMEOUT)

                _progress.value = TransferProgress(
                    state = TransferState.TRANSFERRING,
                    currentFile = "Text message"
                )

                val outputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
                val textBytes = text.toByteArray(Charsets.UTF_8)

                outputStream.writeUTF("__text__")
                outputStream.writeUTF("text/plain")
                outputStream.writeLong(textBytes.size.toLong())
                outputStream.write(textBytes)
                outputStream.flush()

                socket.close()

                _progress.value = TransferProgress(state = TransferState.COMPLETED)

            } catch (e: Exception) {
                Log.e(TAG, "Send text error", e)
                _progress.value = TransferProgress(
                    state = TransferState.FAILED,
                    errorMessage = e.message
                )
            }
        }
    }

    private suspend fun handleIncomingConnection(socket: Socket) {
        try {
            _progress.value = TransferProgress(state = TransferState.TRANSFERRING)

            val inputStream = DataInputStream(BufferedInputStream(socket.getInputStream()))

            val fileName = inputStream.readUTF()
            val mimeType = inputStream.readUTF()
            val fileSize = inputStream.readLong()

            _progress.value = TransferProgress(
                state = TransferState.TRANSFERRING,
                currentFile = fileName,
                totalBytes = fileSize
            )

            if (fileName == "__text__") {
                val textBytes = ByteArray(fileSize.toInt())
                inputStream.readFully(textBytes)
                val receivedText = String(textBytes, Charsets.UTF_8)
                Log.i(TAG, "Received text: $receivedText")
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                val tapShareDir = File(downloadsDir, "TapShare")
                tapShareDir.mkdirs()
                val outputFile = File(tapShareDir, fileName)

                FileOutputStream(outputFile).use { fos ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalReceived = 0L

                    while (totalReceived < fileSize) {
                        val toRead = minOf(BUFFER_SIZE.toLong(), fileSize - totalReceived).toInt()
                        bytesRead = inputStream.read(buffer, 0, toRead)
                        if (bytesRead == -1) break

                        fos.write(buffer, 0, bytesRead)
                        totalReceived += bytesRead

                        _progress.value = TransferProgress(
                            state = TransferState.TRANSFERRING,
                            progress = totalReceived.toFloat() / fileSize,
                            currentFile = fileName,
                            bytesTransferred = totalReceived,
                            totalBytes = fileSize
                        )
                        updateNotification("Receiving: ${(totalReceived * 100 / fileSize)}%")
                    }
                }

                Log.i(TAG, "File saved: ${outputFile.absolutePath}")
            }

            socket.close()
            _progress.value = TransferProgress(state = TransferState.COMPLETED)
            showReceivedNotification(fileName)

        } catch (e: Exception) {
            Log.e(TAG, "Receive error", e)
            _progress.value = TransferProgress(
                state = TransferState.FAILED,
                errorMessage = e.message
            )
        }
    }

    fun resetProgress() {
        _progress.value = TransferProgress()
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, TapShareApp.CHANNEL_TRANSFER)
            .setContentTitle("TapShare")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_tap_share)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showReceivedNotification(fileName: String) {
        val notification = NotificationCompat.Builder(this, TapShareApp.CHANNEL_RECEIVED)
            .setContentTitle("File Received")
            .setContentText(fileName)
            .setSmallIcon(R.drawable.ic_tap_share)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(RECEIVED_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "FileTransferService"
        private const val NOTIFICATION_ID = 1001
        private const val RECEIVED_NOTIFICATION_ID = 1002
        private const val BUFFER_SIZE = 8192
        private const val CONNECTION_TIMEOUT = 10000
        const val DEFAULT_PORT = 8988
    }
}
