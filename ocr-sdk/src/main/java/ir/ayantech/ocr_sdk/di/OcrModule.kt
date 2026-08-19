package ir.ayantech.ocr_sdk.di

import ir.ayantech.networking.datasource.OcrRemoteDataSource
import ir.ayantech.networking.datasource.impl.OcrRemoteDataSourceImpl
import ir.ayantech.networking.repository.OcrRepository
import ir.ayantech.networking.repository.impl.OcrRepositoryImpl
import ir.ayantech.networking.v2.AyanApi
import ir.ayantech.ocr_sdk.domain.usecase.GetCardOcrResultUseCase
import ir.ayantech.ocr_sdk.domain.usecase.UploadCardOcrUseCase
import ir.ayantech.ocr_sdk.ui.viewmodel.OcrViewModel
import ir.ayantech.ocr_sdk.tools.OCRConstant
import org.koin.dsl.module
import ir.ayantech.networking.ayanModel.LogLevel
import ir.ayantech.networking.ayanModel.Language
import ir.ayantech.ocr_sdk.domain.usecase.impl.GetCardOcrResultUseCaseImpl
import ir.ayantech.ocr_sdk.domain.usecase.impl.UploadCardOcrUseCaseImpl
import org.koin.core.module.dsl.viewModel
import kotlin.time.Duration.Companion.seconds

val ocrModule = module {
    single<AyanApi> {
        AyanApi.Builder(context = get(), baseUrl = OCRConstant.Base_URL)
            .setInvokeUserToken { OCRConstant.Token }
            .setTimeOutDuration(120.seconds)
            .setLogLevel(LogLevel.LOG_ALL)
            .setAcceptLanguage(Language.PERSIAN)
            .build()
    }

    single<OcrRemoteDataSource> { OcrRemoteDataSourceImpl(get()) }
    single<OcrRepository> { OcrRepositoryImpl(get()) }

    factory<UploadCardOcrUseCase> { UploadCardOcrUseCaseImpl(get()) }
    factory<GetCardOcrResultUseCase> { GetCardOcrResultUseCaseImpl(get()) }

    viewModel { OcrViewModel(get(), get()) }
}
