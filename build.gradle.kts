import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.util.*

plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.allopen") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("io.quarkus")

    id("io.smallrye.openapi") version "4.1.1"
    id("org.openapi.generator") version "7.14.0"

    idea
}

group = "apps.chocolatecakecodes.cotemplate"
version = "0.1.0"

val urlSubPath = "/cotemplate"

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
    implementation("io.quarkus:quarkus-elytron-security-common")
    implementation("io.quarkus:quarkus-security")
    implementation("io.quarkus:quarkus-cache")
    implementation("io.quarkus:quarkus-arc")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("io.quarkiverse.quinoa:quarkus-quinoa:2.6.2")

    implementation("com.sksamuel.scrimage:scrimage-core:4.3.3")

    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("io.rest-assured:kotlin-extensions")
    testImplementation("io.quarkus:quarkus-jdbc-h2")
    testImplementation("io.kotest:kotest-assertions-core:6.0.0")
}

quarkus {
    this.set("http.root-path", urlSubPath)
}

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

tasks.named("quarkusAppPartsBuild") {
    dependsOn("genApiJs")

    fileTree(project.projectDir.path + "/src/main/webui/").exclude {
        it.path.contains("node_modules/")
            || it.path.contains(".svelte-kit/")
    }.let { inputs.files(it) }
}

tasks.register<GenerateTask>("genApiJs") {
    group = "ui"
    dependsOn("generateOpenApiSpec")

    this.generatorName = "typescript-fetch"
    this.inputSpec.set(project.layout.buildDirectory.file("generated/openapi/openapi.json").get().asFile.path)
    this.outputDir.set(project.projectDir.path + "/src/main/webui/src/lib/js/api")
    this.configOptions.putAll(mapOf(
        Pair("fileNaming", "kebab-case"),
        Pair("modelPropertyNaming", "original"),
        Pair("paramNaming", "original"),
        Pair("useSingleRequestParameter", "false"),
    ))
}

tasks.register<Exec>("buildUi") {
    group = "ui"
    dependsOn("genApiJs")

    workingDir = File(project.projectDir, "src/main/webui")
    commandLine("npm", "run", "build")
}

tasks.register<Tar>("createTar") {
    dependsOn("assemble")

    from(fileTree(project.layout.buildDirectory.dir("quarkus-app")))
    destinationDirectory = project.layout.buildDirectory.dir("dist")
    archiveBaseName = "cotemplate"
    compression = Compression.GZIP
}

tasks.register("uploadPackage") {
    group = "publish"
    dependsOn("createTar")

    doLast {
        val glHost = System.getenv("GITLAB_HOST")
        if(glHost.isNullOrEmpty())
            throw IllegalArgumentException("missing GITLAB_HOST")
        val glProject = System.getenv("GITLAB_PROJECT_ID")
        if(glProject.isNullOrEmpty())
            throw IllegalArgumentException("missing GITLAB_PROJECT_ID")
        val glTokenName = System.getenv("GITLAB_DEPLOY_TOKEN_NAME")
        if(glTokenName.isNullOrEmpty())
            throw IllegalArgumentException("missing GITLAB_DEPLOY_TOKEN_NAME")
        val glTokenKey = System.getenv("GITLAB_DEPLOY_TOKEN_KEY")
        if(glTokenKey.isNullOrEmpty())
            throw IllegalArgumentException("missing GITLAB_DEPLOY_TOKEN_KEY")

        val distFile: File = tasks.named("createTar").get().outputs.files.singleFile
        val glUrl = "https://$glHost/api/v4/projects/$glProject/packages/generic/${project.name}/${project.version}/${distFile.name}"
        val glAuth = "$glTokenName:$glTokenKey"

        val http = URI(glUrl).toURL().openConnection() as HttpURLConnection
        http.apply {
            doOutput = true
            doInput = true
            instanceFollowRedirects = true
            requestMethod = "PUT"
            setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(glAuth.encodeToByteArray()))
        }

        Files.newInputStream(distFile.toPath()).use { inp ->
            inp.transferTo(http.outputStream)
        }
        http.outputStream.close()

        val respStatus = http.responseCode
        if(respStatus < 200 || respStatus > 299) {
            val errStr = http.errorStream.bufferedReader().use { it.readText() }
            throw IOException("Unable to upload package; respStatus = $respStatus\n$errStr")
        } else {
            logger.info("uploaded package ${distFile.name}")
        }
    }
}
