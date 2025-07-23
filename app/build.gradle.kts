plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.iconbiztechnologies1.mynetcape"
    compileSdk = 35

    buildFeatures {
        viewBinding = true
        buildConfig = true  // Added to enable BuildConfig generation
    }

    defaultConfig {
        applicationId = "com.iconbiztechnologies1.mynetcape"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // AndroidX Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase (Updated to latest version)
    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")

    // Picasso (For Image Loading)
    implementation("com.squareup.picasso:picasso:2.8")

    // Secure Password Hashing
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Kotlin Coroutines for Firebase (Optional)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Glide for image loading
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")
    // Material Design Components
    implementation ("com.google.android.material:material:1.9.0")
    // CardView
    implementation ("androidx.cardview:cardview:1.0.0")
    // RecyclerView
    implementation ("androidx.recyclerview:recyclerview:1.3.0")
}