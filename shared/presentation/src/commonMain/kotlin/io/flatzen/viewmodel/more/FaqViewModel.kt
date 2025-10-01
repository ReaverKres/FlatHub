package io.flatzen.viewmodel.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.flatzen.commoncomponents.commonentities.more.FaqConfigData
import io.flatzen.firebase.ConfigFields
import io.flatzen.firebase.ConfigFieldsChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FaqViewModel(
    private val configFieldsChecker: ConfigFieldsChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow<FaqUiState>(FaqUiState.Loading)
    val uiState: StateFlow<FaqUiState> = _uiState

    init {
        getFaqConfig()
    }

    private fun getFaqConfig() {
        viewModelScope.launch(Dispatchers.Default) {
            val result = configFieldsChecker.checkJson<FaqConfigData>(ConfigFields.FaqConfigData)
            if (result != null) {
                _uiState.value = FaqUiState.Success(result)
            } else {
                _uiState.value = FaqUiState.Error(
                    IllegalStateException(
                        "FaqConfigData Что то пошло не так"
                    )
                )
            }
        }
    }
}

sealed class FaqUiState {
    object Loading : FaqUiState()
    data class Success(val faqConfigData: FaqConfigData) : FaqUiState()
    data class Error(val exception: Exception?) : FaqUiState()
}