plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.devtools.ksp")
    id ("kotlin-parcelize")
//    id("com.google.gms.google-services")
}

android {
    namespace = "com.dsk.myexpense"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dsk.myexpense"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true
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
    buildFeatures{
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.navigation.ktx)
    implementation(libs.navigation.ui)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)

    implementation(libs.androidx.room.runtime) // Use your Room version
    ksp(libs.androidx.room.compiler)
    // Optional: For Kotlin coroutine support
    implementation(libs.androidx.room.ktx)

    implementation (libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation (libs.androidx.recyclerview.v121)
    implementation (libs.androidx.lifecycle.viewmodel.ktx.v241)
    implementation (libs.androidx.cardview)

    implementation (libs.androidx.lifecycle.livedata.ktx)

    implementation(libs.kotlinx.serialization.json)

    implementation (libs.androidx.activity.ktx)

    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation (libs.gson)
    implementation (libs.androidx.datastore.preferences) // Add this line

    implementation (libs.androidx.activity.ktx.v1100)
    implementation (libs.androidx.fragment.ktx)

}