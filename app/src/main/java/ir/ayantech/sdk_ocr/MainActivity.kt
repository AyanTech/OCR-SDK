package ir.ayantech.sdk_ocr

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import androidx.core.os.LocaleListCompat
import ir.ayantech.ocr_sdk.data.model.OcrSdkCaptureConfig
import ir.ayantech.ocr_sdk.data.model.OcrSdkLanguage
import ir.ayantech.ocr_sdk.data.model.OcrSdkOcrConfig
import ir.ayantech.ocr_sdk.data.model.OcrSdkOcrDataResult
import ir.ayantech.ocr_sdk.data.model.OcrSdkTextBlock
import ir.ayantech.ocr_sdk.data.model.OcrSdkUriDataResult
import ir.ayantech.ocr_sdk.enums.OcrSdkOcrCardTypesEnum
import ir.ayantech.ocr_sdk.tools.CaptureContract
import ir.ayantech.ocr_sdk.tools.OCRConfig
import ir.ayantech.ocr_sdk.tools.OCRContract
import ir.ayantech.sdk_ocr.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val ocrContract = registerForActivityResult(OCRContract(), ::showOcrResult)
    private val captureContract = registerForActivityResult(CaptureContract(), ::showCaptureResult)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val language = selectedLanguage()
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != language.languageTag) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(language.languageTag)
            )
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        configureControls(language)
    }

    private fun configureControls(language: OcrSdkLanguage) = with(binding) {
        languageGroup.check(
            if (language == OcrSdkLanguage.PERSIAN) R.id.language_persian else R.id.language_english
        )
        languageGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = if (checkedId == R.id.language_english) {
                OcrSdkLanguage.ENGLISH
            } else {
                OcrSdkLanguage.PERSIAN
            }
            getPreferences(MODE_PRIVATE).edit { putString(LANGUAGE_KEY, selected.languageTag) }
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(selected.languageTag)
            )
        }

        cardTypeGroup.check(R.id.card_vehicle)
        switchTwoPhotos.isChecked = true
        switchDarkMode.isChecked = AppCompatDelegate.getDefaultNightMode() ==
            AppCompatDelegate.MODE_NIGHT_YES
        btnOpenOcr.setOnClickListener {
            if (initializeSdk(requireCredentials = true)) ocrContract.launch(ocrConfig())
        }
        btnCapture.setOnClickListener {
            if (initializeSdk(requireCredentials = false)) captureContract.launch(captureConfig())
        }
    }

    private fun selectedLanguage(): OcrSdkLanguage =
        when (getPreferences(MODE_PRIVATE).getString(LANGUAGE_KEY, null)) {
            OcrSdkLanguage.ENGLISH.languageTag -> OcrSdkLanguage.ENGLISH
            else -> OcrSdkLanguage.PERSIAN
        }

    private fun selectedCardType(): OcrSdkOcrCardTypesEnum =
        when (binding.cardTypeGroup.checkedRadioButtonId) {
            R.id.card_bank -> OcrSdkOcrCardTypesEnum.BankCard
            R.id.card_national -> OcrSdkOcrCardTypesEnum.NationalCard
            else -> OcrSdkOcrCardTypesEnum.VehicleCard
        }

    private fun ocrConfig(): OcrSdkOcrConfig {
        val cardType = selectedCardType()
        val instruction = when (cardType) {
            OcrSdkOcrCardTypesEnum.VehicleCard -> R.string.ocr_sample_vehicle_instruction
            OcrSdkOcrCardTypesEnum.NationalCard -> R.string.ocr_sample_national_instruction
            OcrSdkOcrCardTypesEnum.BankCard -> R.string.ocr_sample_second_title
        }
        return OcrSdkOcrConfig(
            maxBase64Mb = 1.0,
            minBase64Mb = 0.2,
            className = MainActivity::class.java.name,
            cardType = cardType.value,
            singlePhoto = !binding.switchTwoPhotos.isChecked,
            extraInfo = binding.inputExtraInfo.text.toString(),
            language = selectedLanguage(),
            nightMode = if (binding.switchDarkMode.isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            },
            textBlock = OcrSdkTextBlock(
                title = R.string.ocr_sample_title,
                secondTitle = instruction,
                firstImageHolderText = R.string.ocr_sample_front,
                secondImageHolderText = R.string.ocr_sample_back,
                buttonText = R.string.ocr_sample_confirm
            )
        )
    }

    private fun captureConfig() = OcrSdkCaptureConfig(
        className = MainActivity::class.java.name,
        extraInfo = binding.inputExtraInfo.text.toString(),
        language = selectedLanguage(),
        textBlock = OcrSdkTextBlock(
            title = R.string.ocr_sample_capture_title,
            firstImageHolderText = R.string.ocr_sample_front,
            buttonText = R.string.ocr_sample_confirm
        )
    )

    private fun showOcrResult(result: OcrSdkOcrDataResult?) {
        binding.imagePreview.visibility = View.GONE
        binding.txtResult.text = if (result == null) {
            getString(R.string.ocr_sample_cancelled)
        } else {
            buildString {
                appendLine(getString(R.string.ocr_sample_result_card_type, result.cardType.orEmpty()))
                appendLine(getString(R.string.ocr_sample_result_extra_info, result.extraInfo.orEmpty()))
                if (result.items.isEmpty()) {
                    append(getString(R.string.ocr_sample_no_fields))
                } else {
                    result.items.forEach { item ->
                        appendLine(
                            getString(
                                R.string.ocr_sample_result_item,
                                item.key.orEmpty(),
                                item.value.orEmpty()
                            )
                        )
                    }
                }
            }.trimEnd()
        }
    }

    private fun showCaptureResult(result: OcrSdkUriDataResult?) {
        binding.txtResult.text = if (result == null) {
            getString(R.string.ocr_sample_cancelled)
        } else {
            getString(R.string.ocr_sample_result_uri, result.uri?.toString().orEmpty()) + "\n" +
                getString(R.string.ocr_sample_result_extra_info, result.extraInfo.orEmpty())
        }
        binding.imagePreview.visibility = if (result?.uri == null) View.GONE else View.VISIBLE
        binding.imagePreview.setImageURI(result?.uri)
    }

    private fun initializeSdk(requireCredentials: Boolean): Boolean {
        val baseUrlInput = binding.inputBaseUrl.text.toString().trim()
        val baseUrl = if (baseUrlInput.isEmpty()) "" else baseUrlInput.trimEnd('/') + "/"
        val token = binding.inputToken.text.toString().trim()
        if (requireCredentials && (baseUrl.isEmpty() || token.isEmpty())) {
            binding.txtResult.setText(R.string.ocr_sample_configuration_required)
            return false
        }
        OCRConfig.builder()
            .setContext(applicationContext)
            .setApplicationID(packageName)
            .setBaseUrl(baseUrl)
            .setToken(token)
            .build()
        return true
    }

    private companion object {
        const val LANGUAGE_KEY = "sample_language"
    }
}
