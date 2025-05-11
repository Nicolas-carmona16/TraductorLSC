package co.edu.udea.compumovil.gr09_20251.traductorlsc.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import co.edu.udea.compumovil.gr09_20251.traductorlsc.navigation.AppRoutes
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.components.AppHeader
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.SecondBlue
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.TraductorLSCTheme

@Composable
fun MethodSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AppHeader()

        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .padding(16.dp)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Selecciona un método de traducción",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(80.dp))

            Button(
                onClick = { navController.navigate(AppRoutes.VIDEO_UPLOAD) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondBlue,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(40.dp)
            ) {
                Text(text = "Cargar vídeo", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(80.dp))

            Button(
                onClick = { navController.navigate(AppRoutes.CAMERA) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondBlue,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(40.dp)
            ) {
                Text(text = "Usar cámara", fontSize = 18.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MethodSelectionScreenPreview() {
    TraductorLSCTheme {
        MethodSelectionScreen(rememberNavController())
    }
}