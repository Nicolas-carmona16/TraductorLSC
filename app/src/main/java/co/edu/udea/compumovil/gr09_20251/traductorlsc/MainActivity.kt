package co.edu.udea.compumovil.gr09_20251.traductorlsc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.FirstBlue
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.SecondBlue
import coil.compose.AsyncImage
import co.edu.udea.compumovil.gr09_20251.traductorlsc.ui.theme.TraductorLSCTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TraductorLSCTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = FirstBlue,
            shape = RoundedCornerShape(bottomEnd = 75.dp, bottomStart = 75.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .fillMaxHeight(0.18f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Text(
                    text = "Traducción de Lenguaje de Señas Colombiana",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        Text(
            text = "Facilitamos la comunicación entre personas sordas y oyentes con una aplicación que traduce la Lengua de Señas Colombiana (LSC) a texto en tiempo real. Usa tu cámara o carga un video para obtener una traducción precisa y rápida.",
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = { /* TODO: Navegar a la siguiente pantalla */ },
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.13f),
            colors = ButtonDefaults.buttonColors(
                containerColor = SecondBlue,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(text = "Iniciar Traducción", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(60.dp))

        AsyncImage(
            model = R.drawable.animacion,
            contentDescription = "Animacion",
            modifier = Modifier
                .fillMaxWidth(1f)
                .height(200.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    TraductorLSCTheme {
        MainScreen()
    }
}