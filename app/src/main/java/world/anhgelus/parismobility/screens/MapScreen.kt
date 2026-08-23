package world.anhgelus.parismobility.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.ui.Image

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.plan_metro),
        contentDescription = "Plan du métro",
        modifier = modifier,
    )
}