package com.scimsoft.tap2share

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.scimsoft.tap2share.model.TransferProgress
import com.scimsoft.tap2share.transfer.FileTransferService
import com.scimsoft.tap2share.ui.screens.*
import com.scimsoft.tap2share.ui.theme.TapShareTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var transferService: FileTransferService? = null
    private var serviceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as FileTransferService.TransferBinder).getService()
            transferService = service
            viewModel.bindTransferService(service)
            serviceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            transferService = null
            serviceBound = false
        }
    }

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setShareFile(it) }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setShareImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.nfcManager.initialize(this)

        val serviceIntent = Intent(this, FileTransferService::class.java)
        startService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        handleIncomingIntent(intent)

        setContent {
            TapShareTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TapShareNavigation(viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.nfcManager.checkNfcState(this)
        viewModel.nfcManager.enableForegroundDispatch(this)
    }

    override fun onPause() {
        super.onPause()
        viewModel.nfcManager.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (serviceBound) {
            unbindService(serviceConnection)
            serviceBound = false
        }
    }

    private fun handleIncomingIntent(intent: Intent) {
        when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED,
            NfcAdapter.ACTION_TECH_DISCOVERED,
            NfcAdapter.ACTION_TAG_DISCOVERED -> {
                vibrateOnTap()
                val transferInfo = viewModel.nfcManager.handleIntent(intent)
                if (transferInfo != null) {
                    Toast.makeText(
                        this,
                        "TapShare: incoming from ${transferInfo.senderName}",
                        Toast.LENGTH_SHORT
                    ).show()
                    viewModel.handleNfcIntent(transferInfo)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun vibrateOnTap() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        }
    }

    fun openFilePicker() {
        filePickerLauncher.launch("*/*")
    }

    fun openImagePicker() {
        imagePickerLauncher.launch("image/*")
    }

    fun openNfcSettings() {
        startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
    }
}

@Composable
fun TapShareNavigation(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val nfcAvailable by viewModel.nfcManager.nfcAvailable.collectAsState()
    val nfcEnabled by viewModel.nfcManager.nfcEnabled.collectAsState()
    val shareItem by viewModel.shareItem.collectAsState()
    val transferProgress by viewModel.transferProgress.collectAsState()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            slideInHorizontally { width -> width } + fadeIn() togetherWith
                    slideOutHorizontally { width -> -width } + fadeOut()
        },
        label = "navigation"
    ) { screen ->
        when (screen) {
            Screen.HOME -> {
                HomeScreen(
                    nfcAvailable = nfcAvailable,
                    nfcEnabled = nfcEnabled,
                    onShareText = { viewModel.navigateTo(Screen.SHARE_TEXT) },
                    onShareFile = { viewModel.navigateTo(Screen.HOME) },
                    onShareImage = { viewModel.navigateTo(Screen.HOME) },
                    onReceive = { viewModel.startReceiving() },
                    onOpenNfcSettings = { }
                )
            }

            Screen.SHARE_TEXT -> {
                ShareTextScreen(
                    onBack = { viewModel.navigateBack() },
                    onShare = { text -> viewModel.setShareText(text) }
                )
            }

            Screen.SENDING -> {
                SendScreen(
                    contentDescription = shareItem?.displayName ?: "Content",
                    transferProgress = transferProgress,
                    onBack = { viewModel.navigateBack() },
                    onCancel = { viewModel.cancelTransfer() }
                )
            }

            Screen.RECEIVING -> {
                ReceiveScreen(
                    transferProgress = transferProgress,
                    onBack = { viewModel.navigateBack() },
                    onCancel = { viewModel.cancelTransfer() }
                )
            }
        }
    }
}
