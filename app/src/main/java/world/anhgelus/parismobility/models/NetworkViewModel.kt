package world.anhgelus.parismobility.models

import androidx.lifecycle.ViewModel
import world.anhgelus.parismobility.data.LinesRepository

class NetworkViewModel(
	repo: LinesRepository,
) : ViewModel() {
	val lines = repo.lines
}
