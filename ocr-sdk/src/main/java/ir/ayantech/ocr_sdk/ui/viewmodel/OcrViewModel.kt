package ir.ayantech.ocr_sdk.ui.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ir.ayantech.networking.v2.api.onFailure
import ir.ayantech.networking.v2.api.onSuccess
import ir.ayantech.ocr_sdk.R
import ir.ayantech.ocr_sdk.data.GetCardOcrResult
import ir.ayantech.ocr_sdk.data.UploadCardOcr
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import ir.ayantech.ocr_sdk.domain.usecase.UploadCardOcrUseCase
import ir.ayantech.ocr_sdk.domain.usecase.impl.EmptyOcrResultException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class OcrUiState {
    object Idle : OcrUiState()
    object Loading : OcrUiState()
    data class UploadSuccess(val fileId: String) : OcrUiState()
    data class ResultSuccess(val response: GetCardOcrResult.GetCardOcrResultResponseModel) :
        OcrUiState()

    data class Error(@StringRes val messageRes: Int) : OcrUiState()
}

class OcrViewModel(
    private val uploadCardOcrUseCase: UploadCardOcrUseCase,
    private val getCardOcrResultUseCase: GetCardOcrResultUseCase,
    private val workDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val uiState: StateFlow<OcrUiState> = _uiState.asStateFlow()
    private var uploadJob: Job? = null
    private var resultJob: Job? = null

    fun uploadCardOcr(images: List<String?>, cardType: String) {
        if (uploadJob?.isActive == true) return
        uploadJob = viewModelScope.launch {
            _uiState.value = OcrUiState.Loading
            try {
                withContext(workDispatcher) {
                    uploadCardOcrUseCase(
                        UploadCardOcr.UploadCardOcrRequestBody(imageArray = images, type = cardType)
                    ).collect { result ->
                        result.onSuccess { output ->
                            _uiState.value = OcrUiState.UploadSuccess(output.fileId)
                        }
                        result.onFailure {
                            _uiState.value = OcrUiState.Error(R.string.ocr_upload_failed)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _uiState.value = OcrUiState.Error(R.string.ocr_upload_failed)
            }
        }
    }

    fun getCardOcrResult(fileId: String) {
        if (resultJob?.isActive == true) return
        resultJob = viewModelScope.launch {
            _uiState.value = OcrUiState.Loading
            try {
                withContext(workDispatcher) {
                    getCardOcrResultUseCase(
                        GetCardOcrResult.GetCardOcrResultRequestBody(fileId = fileId)
                    ).collect { result ->
                        result.onSuccess { response ->
                            _uiState.value = OcrUiState.ResultSuccess(response)
                        }
                        result.onFailure { failure ->
                            val message = if (failure is EmptyOcrResultException)
                                R.string.ocr_result_empty else R.string.ocr_result_failed
                            _uiState.value = OcrUiState.Error(message)
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (exception: Exception) {
                _uiState.value = OcrUiState.Error(
                    if (exception is EmptyOcrResultException) R.string.ocr_result_empty
                    else R.string.ocr_result_failed
                )
            }
        }
    }

    fun resetState() {
        uploadJob?.cancel()
        resultJob?.cancel()
        _uiState.value = OcrUiState.Idle
    }
}
