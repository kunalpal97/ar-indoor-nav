plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.indoornavdisha"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.indoornavdisha"
        minSdk = 24
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
        compose = false
    }
}

//dependencies {
//    implementation(libs.androidx.core.ktx)
//    implementation(libs.androidx.lifecycle.runtime.ktx)
//    implementation(libs.androidx.activity.compose)
//    implementation("androidx.appcompat:appcompat:1.6.1")
//
//    // ARCore
//    implementation("com.google.ar:core:1.36.0")
//
//    // Filament
//    implementation("com.google.android.filament:filament-android:1.50.0")
//    implementation("com.google.android.filament:filament-utils-android:1.50.0")
//}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // sceneview for ar implementation
    implementation("io.github.sceneview:arsceneview:2.2.1")
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("com.google.code.gson:gson:2.8.9")
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")


    // Only ARCore is needed for basic camera functionality
//    implementation("com.google.ar:core:1.39.0")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}


