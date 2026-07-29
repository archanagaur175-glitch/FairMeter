package com.fairmeter.app.ui.incident

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fairmeter.app.ui.theme.ErrorRed
import com.fairmeter.app.ui.theme.TealSecondary
import com.fairmeter.app.ui.theme.WaitingIndicator
import java.util.concurrent.Executors

@Composable
fun IncidentScreen(
    viewModel: IncidentViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture = remember {
        ImageCapture.Builder()
            .setTargetResolution(Size(1920, 1080))
            .build()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Incident Report",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(300.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    val provider = ProcessCameraProvider.getInstance(ctx)
                    provider.addListener({
                        val cameraProvider = provider.get()
                        val preview = Preview.Builder().build()
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        val previewView = androidx.camera.view.PreviewView(ctx)
                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture
                            )
                        } catch (_: Exception) {}

                        previewView
                    }, ContextCompat.getMainExecutor(ctx))
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    val file = viewModel.createPhotoFile()
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture.takePicture(
                        outputOptions,
                        Executors.newSingleThreadExecutor(),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                val uri = viewModel.getPhotoUri(file)
                                viewModel.onPhotoCaptured(uri)
                            }
                            override fun onError(exception: ImageCaptureException) {}
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealSecondary)
            ) {
                Text("Capture Photo")
            }

            Button(
                onClick = { viewModel.shareReport() },
                enabled = state.photoUri != null
            ) {
                Text("Share Report")
            }

            Button(
                onClick = { viewModel.copyReport() },
                colors = ButtonDefaults.buttonColors(containerColor = WaitingIndicator)
            ) {
                Text("Copy")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = state.licensePlate,
            onValueChange = { viewModel.setLicensePlate(it) },
            label = { Text("License Plate") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = state.reportText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
