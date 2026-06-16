package world.anhgelus.parismobility.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import world.anhgelus.parismobility.ui.ScreenTitle

@Preview(showBackground = true)
@Composable
fun NetworkScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        ScreenTitle("Réseau")
    }
}