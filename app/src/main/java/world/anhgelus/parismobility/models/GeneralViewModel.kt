package world.anhgelus.parismobility.models

import android.content.Context
import android.net.ConnectivityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import world.anhgelus.parismobility.data.LinesRepository
import world.anhgelus.parismobility.data.PreferencesRepository
import world.anhgelus.parismobility.data.backend.BackendDataSource
import world.anhgelus.parismobility.data.backend.LongConnection

class GeneralViewModel(ctx: Context) : ViewModel() {
	val backendSource = BackendDataSource(
		LongConnection(
			ctx.getSystemService(ConnectivityManager::class.java)!!
		)
	)
	val linesRepository = LinesRepository(backendSource)
	val preferencesRepository = PreferencesRepository(ctx)
	val isConnected = backendSource.isConnected

	val disruptions = linesRepository.disruptions.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(5_000),
		initialValue = emptyMap()
	)
}