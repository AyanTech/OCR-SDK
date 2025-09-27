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

## ⚙️ Initialization

Configure the SDK in your `Application` class or `MainActivity`:

```kotlin
val config = OCRConfig.builder()
    .setContext(this)
    .setApplicationID("ir.ayantech.sdk_ocr")
    .setBaseUrl("YOUR_BASE_URL")
    .setToken("YOUR_TOKEN")
    .setUploadImageEndPoint("UPLOAD_ENDPOINT")
    .setGetResultEndPoint("RESULT_ENDPOINT")
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

### 2. Encode Image to Base64

```kotlin
val base64 = OcrHelper.encodeImageToBase64(
    context = this,
    imageUri = uri,
    maxBase64Mb = 4.0,        // Max Base64 size (MB)
    minBase64Mb = null,       // Optional min size
    listener = object : EncodeImageListener {
        override fun onSuccess(base64: String) {}
        override fun onFailed(reason: String, throwable: Throwable?) {}
        override fun onProgress(percent: Int, message: String) {}
    }
)
```

---

### 3. Launch OCR

```kotlin
ocrContract.launch(
    OcrSdkOcrConfig(
        maxBase64Mb = 2.5,
        minBase64Mb = 3.5,
        className = packageName,
        cardType = OcrSdkOcrCardTypesEnum.NationalCard.value,
        singlePhoto = true,
        extraInfo = "test",
        textBlock = OcrSdkTextBlock(
            title = "تیتر بالا",
            firstImageHolderText = "عکس اول",
            secondImageHolderText = "عکس دوم",
            buttonText = "کلید پایین"
        )
    )
)
```

---

### 4. Launch with URI

```kotlin
uriContract.launch(
    OcrSdkOcrConfig(
        maxBase64Mb = 2.5,
        minBase64Mb = 3.5,
        className = packageName,
        cardType = OcrSdkOcrCardTypesEnum.VehicleCard.value,
        singlePhoto = false,
        extraInfo = "test",
        textBlock = OcrSdkTextBlock(
            title = "نوشته بالا",
            firstImageHolderText = "عکس اول",
            secondImageHolderText = "عکس دوم",
            buttonText = "باتن پایین"
        )
    )
)
```

---

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
A full working example is provided in the **example module** inside the repository.

---

## 📄 License
Authored by **@Pedram.Fahimi**  
📧 Pedram.Fahimi@gmail.com  
