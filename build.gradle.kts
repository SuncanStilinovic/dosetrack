plugins {
    alias(libs.plugins.android.application) apply false
    // Not applied to modules (AGP 9 compiles Kotlin itself); declared here only to pin the Kotlin Gradle Plugin version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
