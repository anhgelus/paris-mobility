package world.anhgelus.parismobility.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import world.anhgelus.parismobility.R
import world.anhgelus.parismobility.ui.Image

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
	Image(
		painter = painterResource(R.drawable.plan_metro),
		contentDescription = stringResource(R.string.map_alt),
		modifier = modifier,
	)
}