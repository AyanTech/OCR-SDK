# Android OCR SDK Library: Extract Data from VehicleCards, BankCards, and NationalIDs

## Description:

Easily integrate optical character recognition (OCR) capabilities into your Android applications with this powerful SDK. It seamlessly recognizes text from images of VehicleCards, BankCards, and NationalIDs, returning structured data for efficient processing.

## Features:

|  Supported Card Types |
| ------ | 
|VehicleCards |
|BankCards|
|NationalIDs|

### More:
- Base64 Image Input: Capture images and send them in Base64 format for analysis.
- API Endpoint Communication: Establish communication with your designated OCR backend through - endpoints.
- Customization: Override predefined colors and styles for personalized branding.
- Kotlin Language: Developed using Kotlin for modern Android development.
- Open-Source: Available on GitHub for contributions and customization.
  Installation:

> [!NOTE]
> Add the Jitpack repository to your project's settings.gradle file:

Gradle
```sh
repositories {
    maven { url 'https://jitpack.io' }
}
```
> [!NOTE]
>Add the dependency to your app's build.gradle file:

Gradle
```sh
dependencies {
    implementation 'com.github.AyanTech:OCR-SDK:latest-version'
}
```
## Initialization:

Set up the SDK configuration in your application class or main activity:
Kotlin
```sh
val config = OCRConfig.builder()
    .setContext(this)
    .setBaseUrl("replace_with_actual_base_url")
    .setToken("replace_with_actual_token")
    .setUploadImageEndPoint("replace_with_actualt_endpoint")
    .setGetResultEndPoint("replace_with_actual_endpoint")
    .build()
```

## Integration:

Start the OcrActivity and pass required parameters:
Kotlin
```s
startActivity(Intent(this, OcrActivity::class.java).also {
    it.putExtra("cardType", "VehicleCard") // or "BankCard" or "NationalID"
    it.putExtra("className", "your.app.package.MainActivity")
    it.putExtra("extraInfo", "optional data to retrieve") // optional
})
```

## Customization:

Override desired colors and styles in your app's theme (styles.xml):
XML Example:

```s
<color name="ocr_ic_close_tint">#FFFFFFFF</color>
```
## Example Usage:
Refer to the example module in the source code for a complete implementation showcase.

# License:
Authored by @Pedram.Fahimi
Pedram.Fahimi@gmail.com

