plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "io.posidon.android.slablauncher"
        minSdk = 26
        targetSdk = 31
        versionCode = 1
        versionName = "2021.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("com.willowtreeapps:fuzzywuzzy-kotlin-jvm:0.9.0")

    implementation("io.posidon:android.launcherUtils:master-SNAPSHOT")
    implementation("io.posidon:android.loader:master-SNAPSHOT")
    implementation("io.posidon:android.convenienceLib:master-SNAPSHOT")

    implementation("androidx.core:core-ktx:1.6.0")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.fragment:fragment-ktx:1.3.6")
    implementation("androidx.recyclerview:recyclerview:1.2.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    testImplementation("junit:junit:4.13.2")
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}
