
[![](https://jitpack.io/v/AyanTech/OCR-SDK.svg)](https://jitpack.io/#AyanTech/OCR-SDK)

# Android OCR SDK
**Extract structured data from VehicleCards, BankCards, and National IDs with ease.**

---

## 📖 Overview
This SDK enables **fast and reliable OCR integration** into Android apps. It recognizes text from various cards (Vehicle, Bank, and National ID) and returns structured results through a simple API.

---

## 🚀 Features

- ✅ **Supported Card Types**:
  - Vehicle Card
  - Bank Card
  - National Card

- 📷 **Base64 Image Input** – Capture and send images in Base64 format.
- 🌐 **API Endpoint Communication** – Flexible connection to custom OCR backends.
- 🎨 **Customizable UI** – Override colors, styles, and texts to match your brand.
- ⚡ **Modern Development** – Written in Kotlin.
- 🔓 **Open Source** – Available on GitHub for contributions and customization.

---

## 📦 Installation

Add **Jitpack** repository to your `settings.gradle`:

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

Add the SDK dependency to your app's `build.gradle`:

```gradle
dependencies {
    implementation 'com.github.AyanTech:OCR-SDK:latest-version'
}
```

---

## ⚙️ Configuration

To use the SDK, you need to define the API endpoints in your `gradle.properties` file:

```properties
OCR_UPLOAD_PATH="CardOcrUploadImage"
OCR_GET_RESULT_PATH="CardOcrGetResult"
```

## ⚙️ Initialization

Configure the SDK in your `Application` class or `MainActivity`:

```kotlin
val config = OCRConfig.builder()
    .setContext(this)
    .setApplicationID("ir.ayantech.sdk_ocr")
    .setBaseUrl("YOUR_BASE_URL")
    .setToken("YOUR_TOKEN")
    .build()
```

---

## 🔗 Integration

### 1. Register Activity Result Contracts

```kotlin
private val uriContract = registerForActivityResult(CaptureContract()) { result ->
    val uri = result?.uri
    val extraInfo = result?.extraInfo
}

private val ocrContract = registerForActivityResult(OCRContract()) { result ->
    val items = result?.items
    val extraInfo = result?.extraInfo
    val cardType = result?.cardType
}
```

---

### 2. Select the SDK language

Pass your application's selected language to each launch config. The SDK does not select a language from the phone. Both OCR and capture default to Persian for existing callers. If you distribute an Android App Bundle, set `android { bundle { language { enableSplit = false } } }` in the consuming app so both SDK locales are packaged. For application-specific `OcrSdkTextBlock` labels, pass your app's string resource IDs through the `OcrSdkTextBlock` fields and supply `values`/`values-fa` translations. The SDK resolves those IDs in its selected language. The fields accept only `@StringRes` IDs.

```kotlin
val sdkLanguage = if (appLanguageIsPersian) OcrSdkLanguage.PERSIAN else OcrSdkLanguage.ENGLISH
ocrContract.launch(OcrSdkOcrConfig(language = sdkLanguage))
uriContract.launch(OcrSdkCaptureConfig(language = sdkLanguage))
```

### 3. Launch OCR

```kotlin
ocrContract.launch(
    OcrSdkOcrConfig(
        maxBase64Mb = 2.5,
        minBase64Mb = 0.5,
        cardType = OcrSdkOcrCardTypesEnum.NationalCard.value,
        singlePhoto = true,
        language = sdkLanguage,
        textBlock = OcrSdkTextBlock(
            secondTitle = R.string.card_photo_instruction,
            firstImageHolderText = R.string.card_front,
            secondImageHolderText = R.string.card_back,
            buttonText = R.string.confirm
        )
    )
)
```

### 4. Launch with URI

```kotlin
uriContract.launch(OcrSdkCaptureConfig(language = sdkLanguage))
```

## 📥 Getting Results

Results depend on which contract you are using:

---

### 🔹 URI Contract Result

Use this when you only need the **captured image URI**.

| Key        | Type     | Description |
|------------|----------|-------------|
| `uri`      | `Uri?`   | Captured image URI |
| `extraInfo`| `String?`| Custom info passed on launch |

**Example:**

```kotlin
val uri = result?.uri
val extraInfo = result?.extraInfo
```

---

### 🔹 OCR Contract Result

Use this when you perform OCR and expect **extracted card data**.

| Key        | Type              | Description |
|------------|-------------------|-------------|
| `items`    | `List<OcrItem>?`  | OCR extracted fields returned from backend |
| `cardType` | `String?`         | Card type specified at launch (`VehicleCard`, `BankCard`, `NationalCard`) |
| `extraInfo`| `String?`         | Custom info passed on launch |

**Example:**

```kotlin
val items = result?.items
val extraInfo = result?.extraInfo
val cardType = result?.cardType
```

---

## 🎨 Customization

### Colors (`colors.xml`)

```xml
<color name="ocr_ic_close_tint">#FFFFFFFF</color>
<color name="ocr_dialog_color_background">#F2F2F2</color>
<color name="ocr_stroke_button_blue">#2B48EC</color>
<color name="ocr_button_blue">#2B48EC</color>
<color name="ocr_text_button">#FFFFFF</color>
<color name="ocr_camera_fill_color">#2B48EC</color>
<color name="ocr_dash_color">#2BCEFF</color>
```

---

### Text Styles (`textStyles.xml`)

```xml
<style name="ocr_txt_regular_13px_white" parent="txt_regular">
    <item name="android:textColor">@color/ocr_ic_close_tint</item>
</style>

<style name="ocr_txt_regular_14px_gray2" parent="txt_regular">
    <item name="android:textColor">@color/gray_2</item>
</style>
```

---

### Buttons (`styles.xml`)

```xml
<style name="ocr_button" parent="@android:style/Widget.Button">
    <item name="android:gravity">center</item> 
    <item name="android:background">@drawable/ocr_back_blue_button</item>
    <item name="android:textColor">@color/ocr_white</item>
</style>

<style name="ocr_stroked_button" parent="@android:style/Widget.Button">
    <item name="android:gravity">center</item>
    <item name="android:background">@drawable/ocr_back_white_bordered_blue_button</item>
    <item name="android:textColor">@color/ocr_stroke_button_blue</item>
</style>
```

---

### Lottie Animation (`res/raw/`)
```text
ocr_loading.json
```

---

## 📚 Example
The `app` module demonstrates OCR for vehicle, bank, and national cards with one or two photos, capture-only URI output, application-selected English or Persian, custom text and color resources, night mode, and result handling. Enter a backend URL and token on the sample screen to run OCR. Capture-only mode does not require backend credentials. The sample does not save the token.

---

## 📄 License
Authored by **@Pedram.Fahimi**  
📧 Pedram.Fahimi@gmail.com  
