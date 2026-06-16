package world.anhgelus.parismobility

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import world.anhgelus.parismobility.ui.LineKind
import world.anhgelus.parismobility.ui.StopsMonitoring
import world.anhgelus.parismobility.ui.theme.ParisMobiliteTheme
import world.anhgelus.parismobility.ui.theme.Typography

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParisMobiliteTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val defaultModifier = Modifier
                        .padding(innerPadding)
                        .padding(16.dp)
                    Greeting(
                        modifier = defaultModifier
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Votre réseau",
            style = Typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        LineKind(
            modifier = Modifier.padding(horizontal = 16.dp),
            name = "RER",
            lines = listOf("A", "B", "C", "D", "E")
        )
        LineKind(
            modifier = Modifier.padding(horizontal = 16.dp),
            name = "Métro",
            lines = listOf(
                "1",
                "2",
                "3",
                "4",
                "5",
                "6",
                "7",
                "8",
                "9",
                "10",
                "11",
                "12",
                "13",
                "14"
            ),
        )
        Spacer(modifier = Modifier.padding(8.dp))
        StopsMonitoring()
    }
}