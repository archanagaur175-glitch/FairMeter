package com.fairmeter.app.ui.incident

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IncidentUiState(
    val photoUri: Uri? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val licensePlate: String = "",
    val reportText: String = ""
)

class IncidentViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(IncidentUiState())
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun setLocation(lat: Double, lng: Double) {
        _uiState.value = _uiState.value.copy(latitude = lat, longitude = lng)
        updateReportText()
    }

    fun setLicensePlate(plate: String) {
        _uiState.value = _uiState.value.copy(licensePlate = plate)
        updateReportText()
    }

    private fun updateReportText() {
        val state = _uiState.value
        val timestamp = dateFormat.format(Date())
        val text = buildString {
            appendLine("FairMeter Incident Report")
            appendLine("─".repeat(30))
            appendLine("Timestamp: $timestamp")
            appendLine("Location: ${state.latitude}, ${state.longitude}")
            appendLine("Vehicle: ${state.licensePlate.ifBlank { "[not entered]" }}")
            appendLine("─".repeat(30))
            append("Photo: [attached]")
        }
        _uiState.value = state.copy(reportText = text)
    }

    fun onPhotoCaptured(uri: Uri) {
        _uiState.value = _uiState.value.copy(photoUri = uri)
    }

    fun shareReport() {
        val state = _uiState.value
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, state.reportText)
            state.photoUri?.let {
                putExtra(Intent.EXTRA_STREAM, it)
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooser = Intent.createChooser(intent, "Share Incident Report")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(chooser)
    }

    fun copyReport() {
        val clipboard = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("FairMeter Incident Report", _uiState.value.reportText)
        clipboard.setPrimaryClip(clip)
    }

    fun createPhotoFile(): File {
        val dir = File(getApplication<Application>().cacheDir, "incident_photos")
        dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "incident_$timestamp.jpg")
    }

    fun getPhotoUri(file: File): Uri {
        return FileProvider.getUriForFile(
            getApplication(),
            "${getApplication<Application>().packageName}.fileprovider",
            file
        )
    }
}
