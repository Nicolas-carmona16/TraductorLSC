package co.edu.udea.compumovil.gr09_20251.traductorlsc.screens

import android.Manifest
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.components.AppHeader
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.SecondBlue
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import co.edu.udea.compumovil.gr09_20251.traductorlsc.viewmodels.VideoUploadViewModel

@Composable
fun VideoUploadScreen(navController: NavController) {
    val context = LocalContext.current
    var showPermissionRationale by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val viewModel: VideoUploadViewModel = viewModel()
    val selectedVideoUri by viewModel.selectedVideoUri.collectAsState()
    val fileName by viewModel.fileName.collectAsState()

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    DisposableEffect(selectedVideoUri) {
        if (selectedVideoUri != null) {
            val mediaItem = MediaItem.fromUri(selectedVideoUri!!)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
        onDispose { }
    }

    val storagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                var name = ""
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        name = it.getString(nameIndex)
                    }
                }
                viewModel.setVideo(uri, name)
            } ?: run {
                navController.popBackStack()
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            videoPickerLauncher.launch("video/*")
        } else {
            permissionDenied = true
            showPermissionRationale = true
        }
    }

    LaunchedEffect(selectedVideoUri) {
        if (selectedVideoUri == null) {
            when {
                ContextCompat.checkSelfPermission(
                    context,
                    storagePermission
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    videoPickerLauncher.launch("video/*")
                }
                ContextCompat.checkSelfPermission(
                    context,
                    storagePermission
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED -> {
                    showPermissionRationale = true
                }
                else -> {
                    permissionLauncher.launch(storagePermission)
                }
            }
        }
    }

    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = {
                showPermissionRationale = false
                if (permissionDenied) {
                    navController.popBackStack()
                }
            },
            title = { Text("Permiso requerido") },
            text = { Text("Para seleccionar videos necesitamos acceso a tus archivos multimedia") },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionRationale = false
                        permissionLauncher.launch(storagePermission)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondBlue,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Entendido")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionRationale = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppHeader()

        IconButton(
            onClick = {
                exoPlayer.stop()
                viewModel.clearVideo()
                navController.popBackStack()
            },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Volver atrás",
                tint = Color.Black
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            when {
                selectedVideoUri != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .padding(bottom = 24.dp)
                    ) {
                        AndroidView(
                            factory = { context ->
                                PlayerView(context).apply {
                                    player = exoPlayer
                                    useController = true
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Text(
                        "Video seleccionado:",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 20.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondBlue,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Traducir Video", fontSize = 18.sp)
                    }
                }

                permissionDenied -> {
                    Text(
                        "Permiso denegado",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 18.sp
                    )
                }

                else -> {
                    CircularProgressIndicator(color = SecondBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Esperando permisos...")
                }
            }
        }
    }
}