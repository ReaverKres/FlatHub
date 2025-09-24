package io.flatzen.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import io.flatzen.firebase.ConfigManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class SplashScreenViewModel(
    private val configManager: ConfigManager
    ): ViewModel() {
    
    private val _uiState = MutableStateFlow<SplashUiState>(SplashUiState.Loading)
    val uiState: StateFlow<SplashUiState> = _uiState

    init {
        configManager.init()
        fetchRemoteConfig()
    }
    
    private fun fetchRemoteConfig() {
        viewModelScope.launch {
            val result = configManager.fetchAndActivate().last()
            if (result.isSuccess) {
                _uiState.value = SplashUiState.Success()
            } else {
                _uiState.value = SplashUiState.Error(result.exception)
            }
        }
    }
}

sealed class SplashUiState {
    object Loading : SplashUiState()
    class Success() : SplashUiState()
    data class Error(val exception: Exception?) : SplashUiState()
}