plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.allopen") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("io.quarkus")

    idea
}

idea {
    module {
        isDownloadJavadoc = true
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))

    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkus:quarkus-rest-kotlin-serialization")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-hibernate-validator")
    implementation("io.quarkus:quarkus-smallrye-openapi")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus:quarkus-security-jpa")
    implementation("io.quarkus:quarkus-cache")
    implementation("io.quarkus:quarkus-arc")
    //implementation("io.quarkiverse.quinoa:quarkus-quinoa:2.5.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.sksamuel.scrimage:scrimage-core:4.3.3")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")
    testImplementation("io.quarkus:quarkus-jdbc-h2")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

group = "apps.chocolatecakecodes.cotemplate"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<Test> {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}

allOpen {
    annotation("jakarta.ws.rs.Path")
    annotation("jakarta.enterprise.context.ApplicationScoped")
    annotation("jakarta.inject.Singleton")
    annotation("jakarta.persistence.Entity")
    annotation("io.quarkus.test.junit.QuarkusTest")
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
        javaParameters = true
    }
}

tasks.withType<Test>().configureEach {
    this.environment("COTEMPLATE_IMG_STORAGE", "${project.projectDir}/tmp/serverImg-test")
}
