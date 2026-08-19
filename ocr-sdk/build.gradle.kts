import org.gradle.api.publish.maven.MavenPublication

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.parcelize)
    `maven-publish`
}

android {
    namespace = "ir.ayantech.ocr_sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        debug {}
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    implementation(libs.androidx.documentfile)
    implementation(libs.bundles.android.ui)
    implementation(libs.glide)
    implementation(libs.bundles.camerax)
    implementation(libs.networking)
    implementation(libs.picasso)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.circleimageview)
    implementation(libs.whygoogle)
    implementation(libs.lottie)
    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.test.junit)
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
