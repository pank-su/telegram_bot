plugins {
    kotlin("jvm") version "1.9.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.10" apply true
    application
}

group = "su.pank"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    val tgbotapi_version = "9.2.2"
    implementation("dev.inmo:tgbotapi:$tgbotapi_version")
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("org.slf4j:slf4j-log4j12:2.0.9")
    val supabase_version = "1.4.1"
    implementation(platform("io.github.jan-tennert.supabase:bom:$supabase_version"))

    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("io.github.jan-tennert.supabase:functions-kt")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}