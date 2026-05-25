package com.shipmate.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shipmate.ai.ui.callupload.CallUploadScreen
import com.shipmate.ai.ui.callupload.CallUploadViewModel
import com.shipmate.ai.ui.theme.ShipMateTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ShipMateTheme {
                val viewModel: CallUploadViewModel = viewModel()
                CallUploadScreen(viewModel = viewModel)
            }
        }
    }
}
