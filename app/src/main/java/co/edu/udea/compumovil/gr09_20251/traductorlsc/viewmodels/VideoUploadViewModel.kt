package co.edu.udea.compumovil.gr09_20251.traductorlsc.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class VideoUploadViewModel : ViewModel() {

    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri = _selectedVideoUri.asStateFlow()

    private val _fileName = MutableStateFlow("")
    val fileName = _fileName.asStateFlow()

    fun setVideo(uri: Uri?, name: String) {
        _selectedVideoUri.value = uri
        _fileName.value = name
    }

    fun clearVideo() {
        _selectedVideoUri.value = null
        _fileName.value = ""
    }
}
