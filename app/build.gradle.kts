plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ir.ayantech.sdk_ocr"
    compileSdk = 37

    defaultConfig {
        applicationId = "ir.ayantech.sdk_ocr"
        minSdk = 21
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // These belong to the consuming app and are passed to the SDK at runtime.
        for (name in listOf("OCR_UPLOAD_PATH", "OCR_GET_RESULT_PATH")) {
            val value = providers.gradleProperty(name).get()
                .replace("\\", "\\\\").replace("\"", "\\\"")
            buildConfigField("String", name, "\"$value\"")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(project(":ocr-sdk"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.glide)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
}
