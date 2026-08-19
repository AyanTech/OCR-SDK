package ir.ayantech.ocr_sdk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.ayantech.networking.v2.api.onFailure
import ir.ayantech.networking.v2.api.onSuccess
import ir.ayantech.networking.v2.model.ApiCallStatus
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import ir.ayantech.ocr_sdk.domain.usecase.UploadCardOcrUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class OcrUiState {
    object Idle : OcrUiState()
    object Loading : OcrUiState()
    data class UploadSuccess(val fileId: String) : OcrUiState()
    data class ResultSuccess(val response: GetCardOcrResult.GetCardOcrResultResponseModel) : OcrUiState()
    data class Error(val message: String) : OcrUiState()
}

class OcrViewModel(
    private val uploadCardOcrUseCase: UploadCardOcrUseCase,
    private val getCardOcrResultUseCase: GetCardOcrResultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()

    fun uploadCardOcr(images: List<String?>, cardType: String) {
        viewModelScope.launch {
            _uiState.value = OcrUiState.Loading
            uploadCardOcrUseCase(
                UploadCardOcr.UploadCardOcrRequestBody(
                    imageArray = images,
                    type = cardType
                )
            ).collect { result ->
                result.onSuccess { output ->
                    _uiState.value = OcrUiState.UploadSuccess(output.fileId)
                }
                result.onFailure { failure ->
                    _uiState.value = OcrUiState.Error(failure.message ?: "Upload failed")
                }
            }
        }
    }

    fun getCardOcrResult(fileId: String) {
        viewModelScope.launch {
            _uiState.value = OcrUiState.Loading
            getCardOcrResultUseCase(
                GetCardOcrResult.GetCardOcrResultRequestBody(fileId = fileId)
            ).collect { result ->
                result.onSuccess { response ->
                    _uiState.value = OcrUiState.ResultSuccess(response)
                }
                result.onFailure { failure ->
                    _uiState.value = OcrUiState.Error(failure.message ?: "Retrieving result failed")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = OcrUiState.Idle
    }
}
