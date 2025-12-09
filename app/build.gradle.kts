plugins {
    java
    application
    id("com.gradleup.shadow") version "8.3.9"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.kamilszewc:java-ansi-text-colorizer:1.5")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_9
    targetCompatibility = JavaVersion.VERSION_1_9
}

application {
    mainClass.set("org.hugebrain16.App")
}

tasks.shadowJar {
    archiveBaseName.set("raspberry")
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "org.hugebrain16.App"
    }
}