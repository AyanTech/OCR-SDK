# Implementation Plan - Migration to Version Catalog, Kotlin 2.4.0, AGP 9.3.0, and Kotlin DSL

This plan outlines the steps to migrate the `SDK-OCR` project from Groovy-based Gradle scripts to Kotlin DSL, implement a Version Catalog (`libs.versions.toml`), and upgrade the build tools to the requested versions.

## Proposed Changes

### Build Infrastructure

#### [NEW] [libs.versions.toml](file:///Users/farshidroohi/Documents/projects/android/OCR-SDK/gradle/libs.versions.toml)
- Create a central version catalog to manage all dependencies and plugins.
- Define versions for AGP (9.3.0), Kotlin (2.4.0), and other libraries found in the modules.

#### [MODIFY] [settings.gradle.kts](file:///Users/farshidroohi/Documents/projects/android/OCR-SDK/settings.gradle.kts)
- Rename `settings.gradle` to `settings.gradle.kts`.
- Convert the Groovy syntax to Kotlin DSL.
- Ensure `dependencyResolutionManagement` and `pluginManagement` are correctly configured.

#### [MODIFY] [build.gradle.kts (root)](file:///Users/farshidroohi/Documents/projects/android/OCR-SDK/build.gradle.kts)
- Rename root `build.gradle` to `build.gradle.kts`.
- Update plugin declarations to use the Version Catalog.
- Set AGP version to 9.3.0 and Kotlin version to 2.4.0.

### Module Configuration

#### [MODIFY] [build.gradle.kts (:app)](file:///Users/farshidroohi/Documents/projects/android/OCR-SDK/app/build.gradle.kts)
- Rename `app/build.gradle` to `app/build.gradle.kts`.
- Convert the `android` block and `dependencies` block to Kotlin DSL.
- Use libraries and plugins from the Version Catalog.

#### [MODIFY] [build.gradle.kts (:ocr-sdk)](file:///Users/farshidroohi/Documents/projects/android/OCR-SDK/ocr-sdk/build.gradle.kts)
- Rename `ocr-sdk/build.gradle` to `ocr-sdk/build.gradle.kts`.
- Convert the `android`, `publishing`, and `dependencies` blocks to Kotlin DSL.
- Use libraries and plugins from the Version Catalog.

## Dependencies to be Cataloged

I will move the following into `libs.versions.toml`:
- **Plugins**: AGP, Kotlin, Maven Publish, Parcelize, Kapt.
- **Libraries**: AndroidX (Core, AppCompat, ConstraintLayout, Test), Material, Glide, Picasso, OkHttp, Chucker, CameraX, Coroutines, Lottie, and Ayantech libraries.

## Verification Plan

### Automated Tests
- Run `./gradlew clean assembleDebug` to ensure the project builds successfully with the new configuration.
- Run unit tests in both modules: `./gradlew test`.

### Manual Verification
- Verify that the IDE correctly recognizes the Kotlin DSL scripts and the Version Catalog.
- Sync the project in Android Studio.

## Open Questions
- Is there any specific reason for AGP 9.3.0 and Kotlin 2.4.0? I will use exactly these versions as requested.
- The project uses some custom Maven repositories (commented out in `settings.gradle`). I will keep them as they are but in Kotlin DSL syntax.
