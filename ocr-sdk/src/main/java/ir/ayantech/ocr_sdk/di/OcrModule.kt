package ir.ayantech.ocr_sdk.di

import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.networking.ayanModel.LogLevel
import ir.ayantech.networking.datasource.OcrRemoteDataSource
import ir.ayantech.networking.datasource.impl.OcrRemoteDataSourceImpl
import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.repository.impl.OcrRepositoryImpl
import ir.ayantech.networking.v2.AyanApi
import ir.ayantech.ocr_sdk.data.model.OcrSdkLanguage
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import ir.ayantech.ocr_sdk.domain.usecase.UploadCardOcrUseCase
import ir.ayantech.ocr_sdk.domain.usecase.impl.GetCardOcrResultUseCaseImpl
import ir.ayantech.ocr_sdk.domain.usecase.impl.UploadCardOcrUseCaseImpl
import ir.ayantech.ocr_sdk.tools.OCRConstant
import ir.ayantech.ocr_sdk.ui.viewmodel.OcrViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

val ocrModule = module {
    factory<AyanApi> { (language: OcrSdkLanguage) ->
        AyanApi.Builder(context = get(), baseUrl = OCRConstant.baseURL)
            .setInvokeUserToken { OCRConstant.token }
            .setTimeOutDuration(120.seconds)
            .setLogLevel(LogLevel.DO_NOT_LOG)
            .setAcceptLanguage(if (language == OcrSdkLanguage.PERSIAN) Language.PERSIAN else Language.ENGLISH)
            .build()
    }

    factory<OcrRemoteDataSource> { (language: OcrSdkLanguage) ->
        OcrRemoteDataSourceImpl(get { parametersOf(language) })
    }
    factory<OcrRepository> { (language: OcrSdkLanguage) ->
        OcrRepositoryImpl(get { parametersOf(language) })
    }

    factory<UploadCardOcrUseCase> { (language: OcrSdkLanguage) ->
        UploadCardOcrUseCaseImpl(get { parametersOf(language) })
    }
    factory<GetCardOcrResultUseCase> { (language: OcrSdkLanguage) ->
        GetCardOcrResultUseCaseImpl(get { parametersOf(language) })
    }

    viewModel { (language: OcrSdkLanguage) ->
        val repository = get<OcrRepository> { parametersOf(language) }
        OcrViewModel(
            UploadCardOcrUseCaseImpl(repository),
            GetCardOcrResultUseCaseImpl(repository)
        )
    }
}
