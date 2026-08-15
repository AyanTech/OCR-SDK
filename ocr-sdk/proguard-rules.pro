# OCR SDK Specific Rules
-keep public class ir.ayantech.ocr_sdk.model.** { *; }
-keep class ir.ayantech.ocr_sdk.tools.OCRConstant { *; }
-keep class ir.ayantech.ocr_sdk.tools.OcrHelper { *; }
-keep class ir.ayantech.ocr_sdk.tools.Initializer { *; }
-keep class ir.ayantech.ocr_sdk.tools.ConfigBuilder { *; }
-keep class ir.ayantech.ocr_sdk.ui.OcrActivity { *; }
-keep class ir.ayantech.ocr_sdk.databinding.** { *; }

# Ayan Networking Rules
-keep public class ir.ayantech.ayannetworking.** { *; }

# General Android Rules
-keep class android.device.** { *; }
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Signature and Annotations for Reflection (e.g. GSON/Retrofit)
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, Exceptions

##---------------Begin: proguard configuration for glide  ----------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}
##---------------End: proguard configuration for glide  ----------

##---------------Begin: proguard configuration for Gson/Networking  ----------
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
##---------------End: proguard configuration for Gson  ----------

# OkHttp/Retrofit don't-warns
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
