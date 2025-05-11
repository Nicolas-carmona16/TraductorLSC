package co.edu.udea.compumovil.gr09_20251.traductorlsc.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import co.edu.udea.compumovil.gr09_20251.traductorlsc.navigation.AppRoutes
import coil.compose.AsyncImage
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.SecondBlue
import co.edu.udea.compumovil.gr09_20251.traductorlsc.R
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.components.AppHeader
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.TraductorLSCTheme

@Composable
fun MainScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        AppHeader()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Facilitamos la comunicación entre personas sordas y oyentes con una aplicación que traduce la Lengua de Señas Colombiana (LSC) a texto en tiempo real. Usa tu cámara o carga un video para obtener una traducción precisa y rápida.",
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = { navController.navigate(AppRoutes.METHOD_SELECTION) },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(60.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SecondBlue,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(40.dp)
            ) {
                Text(text = "Iniciar Traducción", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(60.dp))

            AsyncImage(
                model = R.drawable.animacion,
                contentDescription = "Animacion",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TraductorLSCTheme {
        MainScreen(rememberNavController())
    }
}