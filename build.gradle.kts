// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        mavenCentral() // add this line
    }
    dependencies {
        classpath("io.realm:realm-gradle-plugin:10.12.0-transformer-api") // add this line
    }
}
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
}