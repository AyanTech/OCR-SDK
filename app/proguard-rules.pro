# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-dontwarn com.coolerfall.download.DownloadCallback
-dontwarn com.coolerfall.download.DownloadManager$Builder
-dontwarn com.coolerfall.download.DownloadManager
-dontwarn com.coolerfall.download.DownloadRequest$Builder
-dontwarn com.coolerfall.download.DownloadRequest
-dontwarn com.coolerfall.download.Downloader
-dontwarn com.coolerfall.download.OkHttpDownloader
-dontwarn com.coolerfall.download.Priority

-keep public class ir.ayantech.ayannetworking.** { *; }
 -keep public class com.alirezabdn.generator.Processor.** { *; }
 -keep public class ir.ayantech.ocr_sdk.model.** { *; }

-keep class android.device.** { *; }

-keep class ir.ayantech.ocr_sdk.ImageViewFragment
-keep class ir.ayantech.ocr_sdk.Constant


# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
# todo don't remove this line, it is for map
#-keep class org.raya.** {*;}
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn java.lang.invoke
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement


-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-keepattributes Exceptions,InnerClasses,Signature

#-keep class au.com.flightcentre.fragment.** { *; }

# Preserve the special static methods that are required in all enumeration classes.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keep class * implements android.os.Serializable {
  public static final android.os.Serializable$Creator *;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}

-keep class androidx.core.app.CoreComponentFactory { *; }
-keep public class ir.ayantech.ayannetworking.** { *; }
-keep public class ir.ayantech.pushnotification.** { *; }
-keep public class ir.ayantech.pushsdk.** { *; }
-keep public class ir.ayantech.versioncontrol.** { *; }
-keep public class ir.ayantech.ghabzino.model.** { *; }
-keep public class ir.ayantech.ghabzino.shaparak.api.** { *; }
-keep public class ir.ayantech.ghabzino.shaparak.model.** { *; }
-keep public class ir.ayantech.ghabzino.helper.AnalyticsHelper.** { *; }
#-keep public class ir.ayantech.ghabzino.GhabzinoApplication
#-keep public class ir.ayantech.ghabzino.storage.** {*;}
#-------------------------------Needed-----------------------------
-keep public class * extends ir.ayantech.whygoogle.** { *; }
-keepclassmembernames class androidx.recyclerview.widget.RecyclerView { *; }
-keepclassmembers class androidx.recyclerview.widget.RecyclerView { *; }
#-keep public class ir.ayantech.whygoogle.activity.SwipableWhyGoogleActivity
-keep public class * extends ir.ayantech.ghabzino.ui.base.BaseBottomSheet
#-keep public class * extends ir.ayantech.ghabzino.ui.base.AyanDialog

##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

##---------------End: proguard configuration for Gson  ----------

##---------------Begin: proguard configuration for Retrofit  ----------
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.-KotlinExtensions
##---------------End: proguard configuration for Retrofit  ----------

##---------------Begin: proguard configuration for okhttp3  ----------
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform
##---------------End: proguard configuration for okhttp3  ----------


##---------------Begin: proguard configuration for okio  ----------
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
##---------------End: proguard configuration for okio  ----------

##---------------Begin: proguard configuration for admob  ----------
# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}

# The following rules are used to strip any non essential Google Play Services classes and method.

# For Google Play Services
-keep public class com.google.android.gms.ads.**{
   public *;
}

# For old ads classes
-keep public class com.google.ads.**{
   public *;
}

# For mediation
-keepattributes *Annotation*

# Other required classes for Google Play Services
# Read more at http://developer.android.com/google/play-services/setup.html
-keep class * extends java.util.ListResourceBundle {
   protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
   public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
   @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
   public static final ** CREATOR;
}
##---------------End: proguard configuration for admob  ----------


-assumenosideeffects class android.util.Log{*;}


##---------------Begin: proguard configuration for glide  ----------
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
##---------------End: proguard configuration for glide  ----------

-keep class com.batch. { *; }
-keep class com.google.android.gms. { *; }
-keep class android.support.v7.app. { *; }
-keep class android.support.v4. { *; }
-dontwarn com.batch.android.mediation.**
-dontwarn com.batch.android.BatchPushService

-dontwarn com.google.android.gms.**
-keepclasseswithmembers class com.camerakit.preview.CameraSurfaceView {
    native <methods>;
}
-dontwarn javax.lang.model.SourceVersion
-dontwarn android.os.ServiceManager*
-dontwarn com.bun.miitmdid.core.MdidSdkHelper*
-dontwarn com.bun.miitmdid.interfaces.IIdentifierListener*
-dontwarn com.bun.miitmdid.interfaces.IdSupplier*
-dontwarn com.google.firebase.iid.FirebaseInstanceId*
-dontwarn com.google.firebase.iid.InstanceIdResult*

-dontwarn com.android.billingclient.**
-dontwarn net.jhoobin.jhub.**

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
#this Is Very Important Rules For Conveting String To Json and visa versa
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
-keep public class * implements java.lang.reflect.Type

-dontwarn com.batch.android.mediation.**
-dontwarn com.batch.android.BatchPushService

-dontwarn okhttp3.
-dontwarn okio.
-dontwarn javax.annotation.
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn java.lang.invoke
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement



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


##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { *; }

# Prevent proguard from stripping interface information from TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
##---------------End: proguard configuration for Gson  ----------

##---------------Begin: proguard configuration for Retrofit  ----------
# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.-KotlinExtensions
##---------------End: proguard configuration for Retrofit  ----------

##---------------Begin: proguard configuration for okhttp3  ----------
# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.ConscryptPlatform
##---------------End: proguard configuration for okhttp3  ----------


##---------------Begin: proguard configuration for okio  ----------
# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*
##---------------End: proguard configuration for okio  ----------


-dontwarn com.batch.android.mediation.**
-dontwarn com.batch.android.BatchPushService

-dontwarn com.android.billingclient.
-dontwarn net.jhoobin.jhub.



-dontwarn com.android.billingclient.**
-dontwarn net.jhoobin.jhub.**


-dontwarn okhttp3.
-dontwarn okio.
-dontwarn javax.annotation.
# A resource is loaded with a relative path so the package of this class must be preserved.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

-dontwarn java.lang.invoke
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement


##---------------Begin: proguard configuration for admob  ----------
# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}

# The following rules are used to strip any non essential Google Play Services classes and method.

# For Google Play Services
-keep public class com.google.android.gms.ads.**{
   public *;
}

# For old ads classes
-keep public class com.google.ads.**{
   public *;
}

# For mediation
-keepattributes *Annotation*

# Other required classes for Google Play Services
# Read more at http://developer.android.com/google/play-services/setup.html
-keep class * extends java.util.ListResourceBundle {
   protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
   public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
   @com.google.android.gms.common.annotation.KeepName *;
}
