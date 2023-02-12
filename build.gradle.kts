import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("org.jetbrains.compose") version "1.3.0"
}

group = "ua.polyakovv"
val version = "1.0.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://mvn.mchv.eu/repository/mchv/")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project.dependencies.platform("it.tdlight:tdlight-java-bom:2.8.9.0"))
    implementation("it.tdlight:tdlight-java")
    implementation("it.tdlight:tdlight-natives-linux-amd64")
    implementation("it.tdlight:tdlight-natives-windows-amd64")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            packageName = "TelegramBot"
            packageVersion = version
        }
    }
}