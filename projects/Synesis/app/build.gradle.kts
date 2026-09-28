import org.gradle.api.JavaVersion

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val releaseVersionName = providers.gradleProperty("versionName").orElse("0.1.0")
val releaseVersionCode = providers.gradleProperty("versionCode").orElse("100")
val releaseKeystorePassword = providers.gradleProperty("releaseKeystorePassword").orElse("synesis")
val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias").orElse("synesis")
val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword").orElse(releaseKeystorePassword)
val signingKeystore = rootProject.file(".signing/synesis-release.p12")

android {
    namespace = "de.konstellarum.synesis"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.konstellarum.synesis"
        minSdk = 26
        targetSdk = 34
        versionCode = releaseVersionCode.get().toInt()
        versionName = releaseVersionName.get()
        resValue("string", "app_name", "Synesis v${releaseVersionName.get()}")
        buildConfigField("String", "UPDATE_REPO_OWNER", "\"aze-the2nd\"")
        buildConfigField("String", "UPDATE_REPO_NAME", "\"blackforest-openforge\"")
        buildConfigField("String", "UPDATE_TAG_PREFIX", "\"synesis-v\"")
        buildConfigField("String", "UPDATE_ASSET_PREFIX", "\"Synesis-v\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        create("release") {
            if (signingKeystore.exists()) {
                storeFile = signingKeystore
                storeType = "PKCS12"
                storePassword = releaseKeystorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (signingKeystore.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":feature:calendar"))
    implementation(project(":feature:notes"))
    implementation(project(":feature:todos"))

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
