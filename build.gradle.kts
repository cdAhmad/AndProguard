import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
// http://www.jetbrains.org/intellij/sdk/docs/basics/getting_started/build_number_ranges.html
dependencies {
    intellijPlatform {
        //intellijIdeaCommunity("2024.2")
        androidStudio("2024.1.2.13")
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.kotlin")
        //plugins("org.jetbrains.android:242.20224.38")
        bundledPlugin("org.jetbrains.android")
        instrumentationTools()
    }
}

intellijPlatform {
    version = "3.6.3"
    group = "com.murphy.proguard"
    buildSearchableOptions = false
    pluginConfiguration {
        ideaVersion.sinceBuild.set("241")
        ideaVersion.untilBuild.set("242.*")
    }
}

// Set the JVM compatibility versions
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    publishPlugin {
        // AndProguard token
        val file = rootProject.file("token.properties")
        val localProperties = loadProperties(file.path)
        val tokenValue = localProperties["token"].toString()
        token.set(tokenValue)
        channels.set(listOf("Stable"))
    }
}
