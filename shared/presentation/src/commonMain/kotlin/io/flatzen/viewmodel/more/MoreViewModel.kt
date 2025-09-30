package io.flatzen.viewmodel.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.flatzen.commoncomponents.commonentities.MoreConfigData
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MoreScreenViewModel(
    private val configFieldsChecker: ConfigFieldsChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow<MoreUiState>(MoreUiState.Loading)
    val uiState: StateFlow<MoreUiState> = _uiState

    init {
        getMoreConfig()
    }

    private fun getMoreConfig() {
        viewModelScope.launch(Dispatchers.Default) {
            val result = configFieldsChecker.checkJson<MoreConfigData>(ConfigFields.MoreConfigData)
            if (result != null) {
                _uiState.value = MoreUiState.Success(result)
            } else {
                _uiState.value = MoreUiState.Error(
                    IllegalStateException(
                        "MoreConfigData Что то пошло не так"
                    )
                )
            }
        }
    }
}

sealed class MoreUiState {
    object Loading : MoreUiState()
    data class Success(val moreConfigData: MoreConfigData) : MoreUiState()
    data class Error(val exception: Exception?) : MoreUiState()
}