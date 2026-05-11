package com.tapshare.app.nfc

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.util.Log
import com.tapshare.app.model.TransferInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.nio.charset.Charset

class NfcManager {

    private val _nfcAvailable = MutableStateFlow(false)
    val nfcAvailable: StateFlow<Boolean> = _nfcAvailable.asStateFlow()

    private val _nfcEnabled = MutableStateFlow(false)
    val nfcEnabled: StateFlow<Boolean> = _nfcEnabled.asStateFlow()

    private var nfcAdapter: NfcAdapter? = null
    private var onTransferInfoReceived: ((TransferInfo) -> Unit)? = null

    fun initialize(activity: Activity) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        _nfcAvailable.value = nfcAdapter != null
        _nfcEnabled.value = nfcAdapter?.isEnabled == true
    }

    fun checkNfcState(activity: Activity) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        _nfcEnabled.value = nfcAdapter?.isEnabled == true
    }

    fun enableForegroundDispatch(activity: Activity) {
        val adapter = nfcAdapter ?: return
        val intent = Intent(activity, activity.javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            activity, 0, intent,
            android.app.PendingIntent.FLAG_MUTABLE
        )
        adapter.enableForegroundDispatch(activity, pendingIntent, null, null)
    }

    fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableForegroundDispatch(activity)
    }

    fun setOnTransferInfoReceived(callback: (TransferInfo) -> Unit) {
        onTransferInfoReceived = callback
    }

    fun createNdefMessage(transferInfo: TransferInfo): NdefMessage {
        val json = JSONObject().apply {
            put("sessionId", transferInfo.sessionId)
            put("senderName", transferInfo.senderName)
            put("itemCount", transferInfo.itemCount)
            put("totalSize", transferInfo.totalSize)
            put("ipAddress", transferInfo.ipAddress)
            put("port", transferInfo.port)
        }

        val mimeRecord = NdefRecord.createMime(
            "application/com.tapshare",
            json.toString().toByteArray(Charset.forName("UTF-8"))
        )

        // Android Application Record ensures the receiver's phone auto-launches
        // TapShare when it detects this NFC message. If TapShare isn't installed,
        // Android opens the Play Store to the app's page.
        val aarRecord = NdefRecord.createApplicationRecord("com.tapshare.app")

        return NdefMessage(arrayOf(mimeRecord, aarRecord))
    }

    fun setupNdefPush(activity: Activity, transferInfo: TransferInfo) {
        val adapter = nfcAdapter ?: return
        val message = createNdefMessage(transferInfo)
        enableForegroundDispatch(activity)
    }

    fun writeNdefToTag(tag: Tag, transferInfo: TransferInfo): Boolean {
        return try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                ndef.connect()
                val message = createNdefMessage(transferInfo)
                ndef.writeNdefMessage(message)
                ndef.close()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write NDEF message", e)
            false
        }
    }

    fun handleIntent(intent: Intent): TransferInfo? {
        return when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED -> {
                parseNdefIntent(intent)
            }
            else -> null
        }
    }

    @Suppress("DEPRECATION")
    private fun parseNdefIntent(intent: Intent): TransferInfo? {
        val rawMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }

        rawMessages?.forEach { raw ->
            val message = raw as NdefMessage
            message.records.forEach { record ->
                if (String(record.type) == "application/com.tapshare" ||
                    record.toMimeType() == "application/com.tapshare"
                ) {
                    return parseTransferInfo(record.payload)
                }
            }
        }
        return null
    }

    private fun parseTransferInfo(payload: ByteArray): TransferInfo? {
        return try {
            val json = JSONObject(String(payload, Charset.forName("UTF-8")))
            TransferInfo(
                sessionId = json.getString("sessionId"),
                senderName = json.getString("senderName"),
                itemCount = json.getInt("itemCount"),
                totalSize = json.getLong("totalSize"),
                ipAddress = json.getString("ipAddress"),
                port = json.getInt("port")
            ).also { info ->
                onTransferInfoReceived?.invoke(info)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse transfer info", e)
            null
        }
    }

    companion object {
        private const val TAG = "NfcManager"
    }
}
