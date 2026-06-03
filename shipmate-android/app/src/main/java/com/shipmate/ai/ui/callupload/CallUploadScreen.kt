package com.shipmate.ai.ui.callupload

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shipmate.ai.data.upload.RecentCallRecordingResponse

@Composable
fun CallUploadScreen(
    viewModel: CallUploadViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val autoUploadPermissions = buildList {
        add(audioPermission)
        add(Manifest.permission.READ_PHONE_STATE)
        add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (autoUploadPermissions.all { grants[it] == true }) {
            viewModel.startAutoUploadMonitor()
        } else {
            viewModel.onAutoUploadPermissionDenied()
        }
    }
    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.selectFile(uri)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.requestAutoUploadConsentIfNeeded()
    }

    LaunchedEffect(
        uiState.autoUploadConsent,
        uiState.autoUploadAttempted,
        uiState.isAutoUploading
    ) {
        if (uiState.autoUploadConsent && !uiState.autoUploadAttempted && !uiState.isAutoUploading) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                audioPermission
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED && (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
            )

            if (hasPermission) {
                viewModel.startAutoUploadMonitor()
            } else {
                viewModel.showAutoUploadPermissionExplanation()
            }
        }
    }

    if (uiState.showAutoUploadConsentDialog) {
        AutoUploadConsentDialog(
            onAgree = viewModel::acceptAutoUploadConsent,
            onDismiss = viewModel::dismissAutoUploadConsentDialog
        )
    }

    if (uiState.showAutoUploadPermissionDialog) {
        AutoUploadPermissionDialog(
            onContinue = {
                viewModel.markAutoUploadPermissionRequestStarted()
                permissionLauncher.launch(autoUploadPermissions)
            },
            onDismiss = viewModel::dismissAutoUploadPermissionExplanation
        )
    }

    Scaffold { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Header() }
                item {
                    ConsentCard(
                        uploadConsent = uiState.uploadConsent,
                        autoUploadConsent = uiState.autoUploadConsent,
                        onUploadConsentChange = viewModel::setConsent,
                        onAutoUploadConsentChange = viewModel::setAutoUploadConsent
                    )
                }
                item {
                    FilePickerCard(
                        selectedFile = uiState.selectedFile,
                        isUploading = uiState.isUploading || uiState.isAutoUploading,
                        onPickFile = { filePicker.launch(arrayOf("audio/*")) },
                        onUpload = viewModel::uploadSelectedFile
                    )
                }
                item { UploadProgressCard(uiState = uiState) }
                item {
                    Text(
                        text = "최근 업로드",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (uiState.recentUploads.isEmpty()) {
                    item { EmptyRecentUploads() }
                } else {
                    items(uiState.recentUploads) { upload ->
                        RecentUploadItem(upload = upload)
                    }
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "통화녹음 업로드",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "출고표 작성을 위해 통화녹음을 업로드하고 처리 상태를 확인합니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AutoUploadConsentDialog(
    onAgree: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "자동 업로드 동의",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "통화 종료 후 새로 생성된 통화녹음만 자동 업로드됩니다. 이전 녹음이나 업로드할 녹음이 있다면 앱에서 수동으로 업로드해 주세요."
            )
        },
        confirmButton = {
            Button(onClick = onAgree) {
                Text("동의")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}

@Composable
private fun AutoUploadPermissionDialog(
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "필요한 권한",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "출고비서는 통화녹음을 자동으로 찾기 위해 오디오 파일, 전화 상태, 연락처, 알림 권한이 필요합니다. 통화 종료 후 새로 생성된 녹음만 자동 업로드되며, 이전 녹음이나 업로드할 녹음이 있다면 앱에서 수동으로 업로드해 주세요."
            )
        },
        confirmButton = {
            Button(onClick = onContinue) {
                Text("권한 허용")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("나중에")
            }
        }
    )
}

@Composable
private fun ConsentCard(
    uploadConsent: Boolean,
    autoUploadConsent: Boolean,
    onUploadConsentChange: (Boolean) -> Unit,
    onAutoUploadConsentChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "업로드 동의",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            ConsentRow(
                checked = uploadConsent,
                onCheckedChange = onUploadConsentChange,
                title = "업로드 및 분석에 동의합니다.",
                description = "선택한 오디오 파일을 업로드하고 출고표 초안 생성을 위해 분석합니다."
            )
            ConsentRow(
                checked = autoUploadConsent,
                onCheckedChange = onAutoUploadConsentChange,
                title = "자동 업로드에 동의합니다.",
                description = "통화 종료 후 새로 생성된 통화녹음만 자동으로 업로드합니다."
            )
            Text(
                text = if (autoUploadConsent) {
                    "자동 업로드 켜짐"
                } else {
                    "자동 업로드 꺼짐"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (autoUploadConsent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilePickerCard(
    selectedFile: SelectedCallFile?,
    isUploading: Boolean,
    onPickFile: () -> Unit,
    onUpload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = selectedFile?.name ?: "선택한 파일이 없습니다.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            if (selectedFile != null) {
                Text(
                    text = "${selectedFile.contentType} - ${formatBytes(selectedFile.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onPickFile,
                    enabled = !isUploading
                ) {
                    Text("파일 선택")
                }
                Button(
                    onClick = onUpload,
                    enabled = !isUploading
                ) {
                    Text(if (isUploading) "업로드 중" else "업로드")
                }
            }
        }
    }
}

@Composable
private fun UploadProgressCard(uiState: CallUploadUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "상태",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = uiState.currentStatus?.label ?: "대기 중",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            LinearProgressIndicator(
                progress = { uiState.uploadProgress },
                modifier = Modifier.fillMaxWidth()
            )
            uiState.errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyRecentUploads() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Text(
            text = "아직 업로드된 통화녹음이 없습니다.",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RecentUploadItem(upload: RecentCallRecordingResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = upload.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = upload.status.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = upload.uploadedAt ?: upload.createdAt ?: "업로드 시간 대기 중",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "크기 정보 없음"
    val mb = bytes / 1024.0 / 1024.0
    return "%.1f MB".format(mb)
}
