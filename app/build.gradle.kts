plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.opendrive"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.opendrive"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

   buildFeatures {
           viewBinding = true // Enable View Binding
   }
}

dependencies {

    implementation(libs.androidx.lifecycle.viewmodel.ktx) // Use latest version
    implementation(libs.androidx.lifecycle.runtime.ktx) // For lifecycleScope
    implementation(libs.androidx.activity.ktx) // For viewModels delegate & permission handling

    implementation(libs.kotlinx.coroutines.android) // Use latest stable

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}