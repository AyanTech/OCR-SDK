import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    id("maven-publish")
}

android {
    namespace = "ir.ayantech.ocr_sdk"
    compileSdk = 37

    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")

        val uploadPath = project.findProperty("OCR_UPLOAD_PATH")
        val getResultPath = project.findProperty("OCR_GET_RESULT_PATH")
        buildConfigField("String", "UPLOAD_PATH", "\"$uploadPath\"")
        buildConfigField("String", "GET_RESULT_PATH", "\"$getResultPath\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug {
            // debuggable = false // Keep as is from original
        }
        release {
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

}

dependencies {
    implementation(libs.androidx.documentfile)
    implementation(libs.bundles.android.ui)
    implementation(libs.glide)

    implementation(libs.ayantech.networking)
    implementation(libs.ayantech.generator)
    ksp(libs.ayantech.generator)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.bundles.camerax)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.circleimageview)
    implementation(libs.lottie)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.core)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.koin.test)
    androidTestImplementation(libs.androidx.junit)

}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.ayantech"
                artifactId = "ocr-sdk"
                version = "1.1.9"
            }
        }
    }
}
