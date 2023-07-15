import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    idea
    application
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("org.jetbrains.kotlin.plugin.spring") version "1.6.21"
    id("org.jetbrains.kotlinx.kover") version "0.5.0"
    id("io.gitlab.arturbosch.detekt") version "1.20.0"
    id("org.springframework.boot") version "2.4.1"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "metrik-backend"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    jcenter {
        content {
            includeGroup("org.jetbrains.kotlinx")
        }
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2020.0.0")
    }
}

// apiTest sourceSets and configuration
idea {
    module {
        sourceDirs = sourceDirs - file("src/apiTest/kotlin")
        testSourceDirs = testSourceDirs + file("src/apiTest/kotlin")
    }
}

ext["log4j2.version"] = "2.17.1"

sourceSets {
    create("apiTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
        java.srcDir("src/apiTest/kotlin")
        resources.srcDir("src/apiTest/kotlin")
    }
}

val apiTestImplementation by configurations.getting {
    extendsFrom(configurations.implementation.get())
}

configurations["apiTestImplementation"].extendsFrom(configurations.testImplementation.get())
configurations["apiTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())
// End of apiTest sourceSets and configuration

// ktlint configuration
val ktlintConfiguration: Configuration by configurations.creating

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.20.0")
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("io.github.openfeign:feign-httpclient:12.4")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.springfox:springfox-boot-starter:3.0.0")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.1")
    implementation("org.springframework.cloud:spring-cloud-starter-sleuth")

    configurations.compile {
        exclude("org.springframework.boot", "spring-boot-starter-logging")
    }

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.2")
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("com.ninja-squad:springmockk:3.0.1")
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
    testImplementation("io.rest-assured:rest-assured:4.4.0")
    testImplementation("io.rest-assured:json-path:4.4.0")
    testImplementation("io.rest-assured:xml-path:4.4.0")
    testImplementation("io.rest-assured:spring-mock-mvc:4.4.0")
    testImplementation("org.codehaus.groovy:groovy:3.0.8")
    testImplementation("org.codehaus.groovy:groovy-json:3.0.8")
    testImplementation("org.codehaus.groovy:groovy-xml:3.0.8")
    testImplementation("org.mock-server:mockserver-junit-jupiter:5.11.1")

    configurations.testCompile {
        exclude("ch.qos.logback", "logback-classic")
    }

    ktlintConfiguration("com.pinterest:ktlint:0.45.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

val coverageExclusion = listOf(
    "metrik/Application**",
    "**/applicationconfig**",
    "**/dto/**",
    "**/vo/**",
    "**/model/**",
    "**/rest/validation/**",
    "**GlobalExceptionHandler**"
)

kover {
    isDisabled = false
    jacocoEngineVersion.set("0.8.8")
    generateReportOnCheck = true
    runAllTestsForProjectTask = false
}

tasks.koverMergedVerify {
    includes = listOf("*")
    excludes = coverageExclusion

    rule {
        name = "Minimal line coverage rate in percent"
        bound {
            minValue = 90
            // valueType is kotlinx.kover.api.VerificationValueType.COVERED_LINES_PERCENTAGE by default
        }
    }
}

tasks.test {
    extensions.configure(kotlinx.kover.api.KoverTaskExtension::class) {
        isDisabled = false
        binaryReportFile.set(file("$buildDir/custom/result.bin"))
        includes = listOf("*")
        excludes = coverageExclusion
    }
}

val apiTest = task<Test>("apiTest") {
    description = "Run API tests."
    group = "verification"
    testClassesDirs = sourceSets["apiTest"].output.classesDirs
    classpath = sourceSets["apiTest"].runtimeClasspath
    shouldRunAfter("test")
}

tasks.check { dependsOn(apiTest) }

detekt {
    buildUponDefaultConfig = true
    allRules = true
    config = files("$projectDir/gradle/detekt/detekt.yml")
    source = objects.fileCollection().from("src")
}

// ktlint tasks
val outputDir = "${project.buildDir}/reports/ktlint/"
val inputFiles = project.fileTree(mapOf("dir" to "src", "include" to "**/*.kt"))

val ktlintCheck = task<JavaExec>("ktlintCheck") {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    description = "Check Kotlin code style."
    classpath = ktlintConfiguration
    group = "verification"
    main = "com.pinterest.ktlint.Main"
    args = listOf("src/**/*.kt")
}

val ktlintFormat by tasks.creating(JavaExec::class) {
    inputs.files(inputFiles)
    outputs.dir(outputDir)

    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = ktlintConfiguration
    mainClass.set("com.pinterest.ktlint.Main")
    args = listOf("-F", "src/**/*.kt")
}
// End of ktlint tasks
