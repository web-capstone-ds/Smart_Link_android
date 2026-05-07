plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.smartfactory.visioninspection"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartfactory.visioninspection"
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
}

dependencies {

    // Eclipse Paho MQTT v5 Java Client (기획서 통신 규약)
    implementation("org.eclipse.paho:org.eclipse.paho.mqttv5.client:1.2.5")

    // Google Gson (백엔드에서 오는 복잡한 JSON 데이터를 Java 객체로 쉽게 변환하기 위함)
    implementation("com.google.code.gson:gson:2.10.1")

    // Historian(데이터) HTTP 통신용 (옵션이지만 거의 필수)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}