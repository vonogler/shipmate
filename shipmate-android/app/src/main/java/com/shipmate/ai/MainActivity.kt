package com.shipmate.ai

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shipmate.ai.data.order.CreateInboundMessageRequest
import com.shipmate.ai.data.upload.ShipmateApiClient
import com.shipmate.ai.ui.callupload.CallUploadScreen
import com.shipmate.ai.ui.callupload.CallUploadViewModel
import com.shipmate.ai.ui.theme.ShipMateTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val pendingSharedOrderKey = "pending_shared_order_text"
    private var sharedOrderText by mutableStateOf("")
    private var isSendingSharedOrder by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedOrderText = getPreferences(MODE_PRIVATE).getString(pendingSharedOrderKey, "").orEmpty()
        handleSharedOrderText(intent)
        setContent {
            ShipMateTheme {
                val viewModel: CallUploadViewModel = viewModel()
                CallUploadScreen(viewModel = viewModel)
                if (sharedOrderText.isNotBlank()) {
                    SharedOrderConfirmDialog(
                        rawText = sharedOrderText,
                        isSending = isSendingSharedOrder,
                        onRawTextChange = { sharedOrderText = it },
                        onDismiss = { sharedOrderText = "" },
                        onSend = { sendSharedOrderText() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSharedOrderText(intent)
    }

    private fun handleSharedOrderText(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND || intent.type != "text/plain") {
            return
        }

        val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (sharedText.isNullOrBlank()) {
            return
        }

        savePendingSharedOrderText(sharedText)
    }

    private fun sendSharedOrderText() {
        val rawText = sharedOrderText.trim()
        if (rawText.isBlank() || isSendingSharedOrder) {
            return
        }

        lifecycleScope.launch {
            isSendingSharedOrder = true
            try {
                ShipmateApiClient.inboundMessageApiService.create(
                    CreateInboundMessageRequest(
                        merchantId = "merchant_local_packaging",
                        merchantStaffId = "staff_android_share",
                        customerId = null,
                        sourceChannel = "SMS_SHARE",
                        businessChannel = "PACKAGING_CUSTOM",
                        rawText = rawText
                    )
                )
                savePendingSharedOrderText("")
                Toast.makeText(this@MainActivity, "주문 원문을 출고비서로 전송했습니다.", Toast.LENGTH_LONG).show()
            } catch (exception: Exception) {
                savePendingSharedOrderText(rawText)
                Toast.makeText(this@MainActivity, "주문 원문 전송에 실패했습니다.", Toast.LENGTH_LONG).show()
            } finally {
                isSendingSharedOrder = false
            }
        }
    }

    private fun savePendingSharedOrderText(rawText: String) {
        sharedOrderText = rawText
        getPreferences(MODE_PRIVATE)
            .edit()
            .putString(pendingSharedOrderKey, rawText)
            .apply()
    }
}

@androidx.compose.runtime.Composable
private fun SharedOrderConfirmDialog(
    rawText: String,
    isSending: Boolean,
    onRawTextChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSend: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isSending) {
                onDismiss()
            }
        },
        title = { Text("공유 주문 확인") },
        text = {
            Column {
                Text("카톡/문자에서 공유한 주문 원문을 확인한 뒤 전송하세요.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = rawText,
                    onValueChange = onRawTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(0.dp),
                    minLines = 6,
                    label = { Text("주문 원문") }
                )
                if (isSending) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = onSend, enabled = !isSending && rawText.isNotBlank()) {
                Text("출고비서로 전송")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isSending) {
                Text("취소")
            }
        }
    )
}
