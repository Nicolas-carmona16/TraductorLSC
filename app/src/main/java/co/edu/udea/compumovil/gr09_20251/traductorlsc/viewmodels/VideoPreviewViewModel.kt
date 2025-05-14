package co.edu.udea.compumovil.gr09_20251.traductorlsc.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoPreviewViewModel : ViewModel() {

    private val _videoUri = MutableStateFlow<Uri?>(null)
    val videoUri: StateFlow<Uri?> = _videoUri.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName: StateFlow<String> = _fileName.asStateFlow()

    fun setVideo(uri: Uri, name: String) {
        _videoUri.value = uri
        _fileName.value = name
    }

    fun clearVideo() {
        _videoUri.value = null
        _fileName.value = ""
    }
}