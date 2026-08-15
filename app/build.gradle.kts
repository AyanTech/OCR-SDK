plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ir.ayantech.sdk_ocr"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.ayantech.sdk_ocr"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // kotlinOptions removed as it defaults to targetCompatibility in AGP 9.3
}

dependencies {
    implementation(libs.whygoogle)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":ocr-sdk"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.glide)
    implementation(libs.ayantech.versioncontrol)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    debugImplementation(libs.chucker.library)
    releaseImplementation(libs.chucker.library.no.op)
}
