// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.android.library") version "9.2.1" apply false
}

extra["compileSdkVersion"] = 35
extra["targetSdkVersion"] = 28
extra["minSdk"] = 21
extra["versionCode"] = 400
extra["versionName"] = "4.0.0"
extra["xVersion"] = "1.1.0"
extra["hiddenApiBypass"] = "4.3"
