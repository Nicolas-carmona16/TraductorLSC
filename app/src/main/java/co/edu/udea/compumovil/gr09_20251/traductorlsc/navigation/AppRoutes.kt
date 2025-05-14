package co.edu.udea.compumovil.gr09_20251.traductorlsc.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import co.edu.udea.compumovil.gr09_20251.traductorlsc.screens.CameraScreen
import co.edu.udea.compumovil.gr09_20251.traductorlsc.screens.MainScreen
import co.edu.udea.compumovil.gr09_20251.traductorlsc.screens.MethodSelectionScreen
import co.edu.udea.compumovil.gr09_20251.traductorlsc.screens.VideoPreviewScreen
import co.edu.udea.compumovil.gr09_20251.traductorlsc.screens.VideoUploadScreen

object AppRoutes {
    const val MAIN_SCREEN = "main_screen"
    const val METHOD_SELECTION = "method_selection"
    const val VIDEO_UPLOAD = "video_upload"
    const val CAMERA = "camera"
    const val VIDEO_PREVIEW = "video_preview?videoUri={videoUri}"

    fun getVideoPreviewRoute(videoUri: String): String {
        return "video_preview?videoUri=$videoUri"
    }
}

fun NavGraphBuilder.appNavigation(navController: NavController) {
    composable(AppRoutes.MAIN_SCREEN) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MainScreen(navController)
        }
    }
    composable(AppRoutes.METHOD_SELECTION) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            MethodSelectionScreen(navController)
        }
    }
    composable(AppRoutes.VIDEO_UPLOAD) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            VideoUploadScreen(navController)
        }
    }
    composable(AppRoutes.CAMERA) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CameraScreen(navController)
        }
    }
    composable(
        route = AppRoutes.VIDEO_PREVIEW,
        arguments = listOf(
            navArgument("videoUri") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }
        )
    ) { backStackEntry ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val videoUri = backStackEntry.arguments?.getString("videoUri")?.toUri()
            VideoPreviewScreen(navController, videoUri)
        }
    }
}