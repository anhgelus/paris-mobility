package world.anhgelus.parismobility.models

import android.content.Context
import androidx.lifecycle.ViewModel
import world.anhgelus.parismobility.data.LinesDataSource
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PreferencesRepository
import world.anhgelus.parismobility.data.backend.BackendDataSource

class GeneralViewModel(
	ctx: Context,
	linesDataSource: LinesDataSource,
	backendDataSource: BackendDataSource,
) : ViewModel() {
	val linesRepository = LinesRepository(
		linesDataSource,
		backendDataSource
	)
	val preferencesRepository = PreferencesRepository(ctx)

	init {
		linesRepository.loadLines()
	}
}