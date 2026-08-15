plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.legacy.kapt)
    id("kotlin-parcelize")
    id("maven-publish")
}

android {
    namespace = "ir.ayantech.ocr_sdk"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("proguard-rules.pro")
    }

    buildFeatures {
        viewBinding = true
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // kotlinOptions removed as it defaults to targetCompatibility in AGP 9.3
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)

    implementation(libs.glide)

    // cameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    implementation(libs.ayantech.networking)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.circleimageview)
    implementation(libs.lottie)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "com.github.ayantech"
                artifactId = "ocr-sdk"
                version = "1.0.3-beta07"
            }
        }
    }
}
