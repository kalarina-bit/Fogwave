plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "cc.skysparkle.fogwave"

  compileSdk { version = release(37) }

  defaultConfig {
    applicationId = "cc.skysparkle.fogwave"
    minSdk = 24
    targetSdk = 36
    versionCode = 11
    versionName = "1.0.11"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Library versions shown under About -> Licenses, taken from the version catalog so they
    // always match what is actually bundled.
    buildConfigField("String", "COMPOSE_BOM_VERSION", "\"${libs.versions.composeBom.get()}\"")
    buildConfigField("String", "ACTIVITY_VERSION", "\"${libs.versions.activityCompose.get()}\"")
    buildConfigField("String", "CORE_KTX_VERSION", "\"${libs.versions.coreKtx.get()}\"")
    buildConfigField("String", "LIFECYCLE_VERSION", "\"${libs.versions.lifecycleRuntimeCompose.get()}\"")
    buildConfigField("String", "MEDIA3_VERSION", "\"${libs.versions.media3.get()}\"")
    buildConfigField("String", "COROUTINES_VERSION", "\"${libs.versions.kotlinxCoroutinesAndroid.get()}\"")
    buildConfigField("String", "KOTLIN_VERSION", "\"${libs.versions.kotlin.get()}\"")
    buildConfigField("String", "COIL_VERSION", "\"${libs.versions.coilCompose.get()}\"")
  }

  // Release signing is applied only when a keystore is available, so anyone (including F-Droid,
  // which signs with its own key) can build an unsigned release from a fresh clone.
  val releaseKeystore = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
  val canSignRelease = releaseKeystore.exists() && System.getenv("STORE_PASSWORD") != null

  signingConfigs {
    if (canSignRelease) {
      create("release") {
        storeFile = releaseKeystore
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
        keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("STORE_PASSWORD")
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Keeps release builds reproducible (F-Droid).
      vcsInfo.include = false
      if (canSignRelease) signingConfig = signingConfigs.getByName("release")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation(libs.coil.compose)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media3.session)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
