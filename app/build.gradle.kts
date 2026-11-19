plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "app.fleasion.mobile"
    compileSdk = 35
    defaultConfig {
        applicationId = "app.fleasion.mobile"
        minSdk = 24
        targetSdk = 35
        versionCode = 600
        versionName = "0.6.0-alpha01"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildTypes { release { isMinifyEnabled = false } }
}
dependencies {
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}
