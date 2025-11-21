import java.util.Properties
import java.io.FileInputStream
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}
val localProps = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
    localProps.load(FileInputStream(localFile))
}
val localProperties = Properties().apply {
    load(project.rootProject.file("local.properties").inputStream())
}

val kakaoKey: String = localProps.getProperty("KAKAO_NATIVE_KEY")?.let { it } ?: "YOUR_KAKAO_KEY_HERE"

android {
    namespace = "com.example.tot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.tot"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "KAKAO_NATIVE_KEY", "\"$kakaoKey\"")
        manifestPlaceholders["KAKAO_NATIVE_KEY"] = kakaoKey
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["mapsApiKey"] = localProperties.getProperty("MAPS_API_KEY") ?: ""
    }
    buildFeatures {
        buildConfig = true
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
}


dependencies {
    implementation("com.google.android.gms:play-services-maps:18.2.0") // 구글 지도 SDK
    implementation("com.google.android.libraries.places:places:3.4.0") // 구글 장소(검색) SDK
    implementation("com.google.android.gms:play-services-location:21.3.0") //안드로이드 위치 서비스
    implementation("com.kakao.sdk:v2-share:2.22.0")  // 공유 메시지 전송
    implementation("com.naver.maps:map-sdk:3.23.0") // 네이버 지도 SDK
    implementation("nl.bryanderidder:themed-toggle-button-group:1.4.1") //토글 버튼 리스트
    implementation("com.github.ismaeldivita:chip-navigation-bar:1.4.0") //하단 네브바
    implementation("androidx.core:core-splashscreen:1.0.1") // 스플래시 이미지
    implementation("com.github.angads25:toggle:1.1.0") // 스위치버튼
    implementation("de.hdodenhof:circleimageview:3.1.0") // 동그란 프로필 이미지
    implementation("com.github.thesurix:gesture-recycler:1.17.0") //드래그 & 드랍, 스와이프 레이아웃

    //파이어베이스
    implementation(platform("com.google.firebase:firebase-bom:34.4.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.google.firebase:firebase-storage")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ✅ Gson 라이브러리 추가 (JSON 직렬화/역직렬화)
    implementation("com.google.code.gson:gson:2.10.1")
}
