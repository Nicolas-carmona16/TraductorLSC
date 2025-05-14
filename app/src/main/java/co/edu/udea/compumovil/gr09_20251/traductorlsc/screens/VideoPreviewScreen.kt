package co.edu.udea.compumovil.gr09_20251.traductorlsc.screens

import android.net.Uri
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.components.AppHeader
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.SecondBlue
import co.edu.udea.compumovil.gr09_20251.traductorlsc.viewmodels.VideoPreviewViewModel
import java.io.File

@Composable
fun VideoPreviewScreen(navController: NavController, videoUri: Uri?) {
    val context = LocalContext.current
    val viewModel: VideoPreviewViewModel = viewModel()
    var fileExists by remember { mutableStateOf(false) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(videoUri) {
        if (videoUri != null) {
            val name = videoUri.lastPathSegment?.let {
                val file = File(it)
                file.name
            } ?: "Video grabado"

            viewModel.setVideo(videoUri, name)
        }
    }

    val videoUriState by viewModel.videoUri.collectAsState()
    val fileName by viewModel.fileName.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearVideo()
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoUriState) {
        if (videoUriState != null) {
            try {
                val mediaItem = MediaItem.fromUri(videoUriState!!)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()
                fileExists = true
            } catch (e: Exception) {
                e.printStackTrace()
                fileExists = false
            }
        } else {
            fileExists = false
        }
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
            if (videoUriState != null && fileExists) {
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
                    "Video grabado:",
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = {
                            exoPlayer.stop()
                            viewModel.clearVideo()
                            navController.popBackStack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Gray,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Volver a grabar", fontSize = 16.sp)
                    }

                    Button(
                        onClick = {
                            exoPlayer.stop()
                            navController.navigate("main_screen") {
                                popUpTo("main_screen") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .padding(start = 8.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondBlue,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Traducir Video", fontSize = 16.sp)
                    }
                }
            } else {
                Text(
                    "No se pudo cargar el video",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SecondBlue,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Volver", fontSize = 18.sp)
                }
            }
        }
    }
}