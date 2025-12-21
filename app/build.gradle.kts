import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.example.calkulatorcnc"
    compileSdk = 36 // Uproszczony zapis standardowy

    defaultConfig {
        applicationId = "kalkulator.cnc"
        minSdk = 27
        targetSdk = 36
        versionCode = 80
        versionName = "3.2.80"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AdMob APP ID dla manifestu (domyślnie testowy)
        val appIdFromProps = localProperties.getProperty("ADMOB_APP_ID") ?: "ca-app-pub-3940256099942544~3347511713"
        manifestPlaceholders += mapOf("admobAppId" to appIdFromProps)
    }

    // 2. Konfiguracja podpisywania (Signing Configs)
    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("MYAPP_RELEASE_STORE_FILE", "debug.keystore"))
            storePassword = localProperties.getProperty("MYAPP_RELEASE_STORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("MYAPP_RELEASE_KEY_ALIAS", "")
            keyPassword = localProperties.getProperty("MYAPP_RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            val appId = localProperties.getProperty("ADMOB_APP_ID") ?: ""
            val bannerId = localProperties.getProperty("ADMOB_BANNER_ID") ?: ""
            val adStartId = localProperties.getProperty("ADMOB_ADSTART_ID") ?: ""

            manifestPlaceholders["admobAppId"] = appId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$bannerId\"")
            buildConfigField("String", "ADMOB_ADSTART_ID", "\"$adStartId\"")

            signingConfig = signingConfigs.getByName("release")
        }

        debug {

            val appId = localProperties.getProperty("ADMOB_APP_ID") ?: "ca-app-pub-3940256099942544~3347511713"
            val bannerId = localProperties.getProperty("ADMOB_BANNER_ID") ?: "ca-app-pub-3940256099942544/6300978111"
            val adStartId = localProperties.getProperty("ADMOB_ADSTART_ID") ?: "ca-app-pub-3940256099942544/9257395915"

            manifestPlaceholders["admobAppId"] = appId
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$bannerId\"")
            buildConfigField("String", "ADMOB_ADSTART_ID", "\"$adStartId\"")

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    //implementation(libs.androidx.activity)
    //implementation(libs.androidx.constraintlayout)
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)
    implementation(libs.bundles.lifecycle)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}