package com.scimsoft.tap2share

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
    }

    @Suppress("DEPRECATION")
    private fun handleShareIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }

                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    action = ACTION_SHARE_CONTENT
                    text?.let { putExtra(EXTRA_SHARE_TEXT, it) }
                    uri?.let { putExtra(EXTRA_SHARE_URI, it.toString()) }
                    putExtra(EXTRA_SHARE_MIME_TYPE, intent.type ?: "*/*")
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(mainIntent)
                finish()
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }

                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    action = ACTION_SHARE_MULTIPLE
                    uris?.let {
                        putParcelableArrayListExtra(EXTRA_SHARE_URIS, ArrayList(it))
                    }
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(mainIntent)
                finish()
            }
        }
    }

    companion object {
        const val ACTION_SHARE_CONTENT = "com.scimsoft.tap2share.ACTION_SHARE_CONTENT"
        const val ACTION_SHARE_MULTIPLE = "com.scimsoft.tap2share.ACTION_SHARE_MULTIPLE"
        const val EXTRA_SHARE_TEXT = "share_text"
        const val EXTRA_SHARE_URI = "share_uri"
        const val EXTRA_SHARE_URIS = "share_uris"
        const val EXTRA_SHARE_MIME_TYPE = "share_mime_type"
    }
}
