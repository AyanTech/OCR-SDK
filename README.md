# Android OCR SDK: Extract Data from VehicleCards, BankCards, and NationalIDs

## Description:

Easily integrate optical character recognition (OCR) capabilities into your Android applications with this powerful SDK. It seamlessly recognizes text from images of VehicleCards, BankCards, and NationalIDs, returning structured data for efficient processing.

## Features:

|  Supported Card Types |
| ------ | 
|VehicleCards |
|BankCards|
|NationalCard|

### More:
- Base64 Image Input: Capture images and send them in Base64 format for analysis.
- API Endpoint Communication: Establish communication with your designated OCR backend through - endpoints.
- Customization: Override predefined colors and styles for personalized branding.
- Kotlin Language: Developed using Kotlin for modern Android development.
- Open-Source: Available on GitHub for contributions and customization.
  Installation:

> [!Important]
> Add the Jitpack repository to your project's settings.gradle file:

Gradle
```sh
repositories {
    maven { url 'https://jitpack.io' }
}
```
> [!Important]
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
Create ocrLuncher:

```s
var ocrLuncher =
registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
if (result.resultCode == Activity.RESULT_OK) {
val data: ArrayList<GetCardOcrResult.Result>? =
result.data?.extras?.getParcelableArrayList("GetCardOcrResult")
val extraInfo =  result.data?.getStringExtra("extraInfo")

            
                        if (data != null) {
                        
                           val chassisNumber =
                                data.find { it.Key == "ChassisNumber" }?.Value.toString()
                                
                            //get other data like example above
                        }
                    )
             }
        }
 ```
        
Start the OcrActivity and pass required parameters:
Kotlin
```s
  val intent = Intent(this, OcrActivity::class.java)
        intent.putExtra("cardType", "VehicleCard") // or "BankCard" or "NationalID"
        intent.putExtra("className", "com.khodropay.dokan.MainActivity")
        intent.putExtra("singlePhoto", false) // true takes a single image
        intent.putExtra("extraInfo", mobileNumber) // optional
        ocrLuncher.launch(intent)
```

## Get Result

Receive desire result form intent in activity like below Example:

| Keys             | Explenaition |
|------------------| ------ | 
| GetCardOcrResult |  Array list containing results from the OCR API |
| cardType         | String representing the exact card type sent during integration |
| extraInfo        | String containing additional information sent during integration |


Kotlin
```s
  val extras = intent.extras
        val data = extras?.getParcelableArrayList<GetCardOcrResult.Result>("GetCardOcrResult")
        val cardType = intent.getStringExtra("cardType")
        val extraInfo = intent?.getStringExtra("extraInfo")
         
```

## Customization:

Override desired colors and styles in your app's theme (styles.xml):
XML Example:

> [!Tip]
>Colors in colors.xml
```s
     <color name="ocr_ic_close_tint">#FFFFFFFF</color>
    <color name="ocr_dialog_color_background">#F2F2F2</color>
    <color name="ocr_stroke_button_blue">#2B48EC</color>
    <color name="ocr_button_blue">#2B48EC</color>
    <color name="default_divider">@color/ocr_dialog_color_background</color>
    <color name="ocr_text_button">#ffffff</color>
    <color name="ocr_camera_fill_color">#2B48EC</color>
    <color name="ocr_gray_7">#D8D8D8</color>
    <color name="ocr_white">#FFFFFF</color>
    <color name="ocr_fragment_background">@color/gray_4</color>
    <color name="ocr_dash_color">#2BCEFF</color>
```

> [!Tip]
>Styles in textStyles.xml
```s
 <style name="ocr_txt_regular_13px_white" parent="txt_regular">
        <item name="android:textColor">@color/ocr_ic_close_tint</item>
    </style>
    
    <style name="ocr_txt_regular_14px_gray2" parent="txt_regular">
        <item name="android:textColor">@color/gray_2</item>
    </style>
```

> [!Tip]
>Styles in styles.xml
```s
       <style name="ocr_button" parent="@android:style/Widget.Button">
        <item name="android:gravity">center</item>
        <item name="android:paddingStart">@dimen/margin_16</item>
        <item name="android:paddingEnd">@dimen/margin_16</item>
        <item name="android:clipToPadding">false</item>
        <item name="android:height">@dimen/ocr_button_height</item>
        <item name="android:background">@drawable/ocr_back_blue_button</item>
        <item name="android:foreground">?android:attr/selectableItemBackground</item>
        <item name="fontFamily">@font/regular</item>
        <item name="android:fontFamily">@font/regular</item>
        <item name="android:textSize">@dimen/txt_regular</item>
        <item name="android:textColor">@color/ocr_white</item>
    </style>

    <style name="ocr_stroked_button" parent="@android:style/Widget.Button">
        <item name="android:gravity">center</item>
        <item name="android:paddingStart">@dimen/margin_16</item>
        <item name="android:paddingEnd">@dimen/margin_16</item>
        <item name="android:clipToPadding">false</item>
        <item name="android:height">@dimen/ocr_button_height</item>
        <item name="android:background">@drawable/ocr_back_white_bordered_blue_button</item>
        <item name="android:foreground">?android:attr/selectableItemBackground</item>
        <item name="fontFamily">@font/regular</item>
        <item name="android:fontFamily">@font/regular</item>
        <item name="android:textSize">14sp</item>
        <item name="android:textColor">@color/ocr_stroke_button_blue</item>
    </style>
    
```
> [!Tip]
>Json file in raw directory
```s
ocr_loading.json
```

## Example Usage:
Refer to the example module in the source code for a complete implementation showcase.

# License:
Authored by @Pedram.Fahimi
Pedram.Fahimi@gmail.com

