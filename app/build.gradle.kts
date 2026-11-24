plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dosetrack"
    // Compose 1.13 / core 1.19 require at least API 37.1 to compile against.
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.dosetrack"
        minSdk = 30 // Android 11: covers phones up to ~5 years old, including un-updated budget devices
        targetSdk = 36
        versionCode = 4
        versionName = "1.3.1"
    }

    // Secrets live in ~/.gradle/gradle.properties (dosetrack.*), never in this repo.
    signingConfigs {
        create("release") {
            storeFile = providers.gradleProperty("dosetrack.storeFile").orNull?.let { file(it) }
            storePassword = providers.gradleProperty("dosetrack.storePassword").orNull
            keyAlias = providers.gradleProperty("dosetrack.keyAlias").orNull
            keyPassword = providers.gradleProperty("dosetrack.keyPassword").orNull
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
}
