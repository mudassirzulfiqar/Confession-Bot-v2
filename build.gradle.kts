plugins {
    kotlin("jvm") version "1.8.10" // Adjust Kotlin version if needed
    id("com.github.johnrengelman.shadow") version "8.1.1" // Shadow plugin for fat JARs
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0") // JDA dependency
    implementation("io.github.cdimascio:dotenv-kotlin:6.4.1") // dotenv for environment variables
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

application {
    // Set the main class for the application
    mainClass.set("ConfessionBotKt") // Replace with your main class
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "ConfessionBotKt" // Ensure the JAR has the correct entry point
    }
}

tasks.shadowJar {
    archiveBaseName.set("confession-bot") // Base name for the fat JAR
    archiveVersion.set("1.0.0") // Version
    archiveClassifier.set("") // No extra classifier (e.g., no "-all")
}