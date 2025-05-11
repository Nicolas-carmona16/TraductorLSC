package co.edu.udea.compumovil.gr09_20251.traductorlsc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import co.edu.udea.compumovil.gr09_20251.traductorlsc.navigation.AppRoutes
import co.edu.udea.compumovil.gr09_20251.traductorlsc.navigation.appNavigation
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.TraductorLSCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TraductorLSCTheme {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = AppRoutes.MAIN_SCREEN
                ) {
                    appNavigation(navController)
                }
            }
        }
    }
}