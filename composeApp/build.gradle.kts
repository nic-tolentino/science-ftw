import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }
    
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(libs.compose.uiToolingPreview)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.splashscreen)

                // Coil: OkHttp network layer for Android
                implementation(libs.coil.network.okhttp)

                // Firebase
                implementation(project.dependencies.platform(libs.firebase.bom))
                implementation(libs.firebase.analytics)
                implementation(libs.firebase.crashlytics)
            }
        }
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain.get())
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)

            dependencies {
                // Coil: Ktor + Darwin engine for iOS
                implementation(libs.coil.network.ktor)
                implementation(libs.ktor.client.darwin)
            }
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Jetpack Navigation for Compose Multiplatform
            implementation(libs.androidx.navigation.compose)

            // kotlin-inject DI runtime
            implementation(libs.kotlin.inject.runtime)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.okio)
            implementation(libs.kotlinx.datetime)

            // Coil for network image loading (AsyncImage composable)
            implementation(libs.coil.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

dependencies {
    // kotlin-inject compiler processing
    add("kspCommonMainMetadata", libs.kotlin.inject.compiler)
    add("kspAndroid", libs.kotlin.inject.compiler)
    add("kspIosSimulatorArm64", libs.kotlin.inject.compiler)
    add("kspIosArm64", libs.kotlin.inject.compiler)
    
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.complexly.academy"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.complexly.academy"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

compose.resources {
    publicResClass = true
}
