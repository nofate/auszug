import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    kotlin("plugin.serialization") version "1.6.20"
    application
}

group = "auszug"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
    }
}

val ktorVersion = "2.0.0"
dependencies {
    implementation(kotlin("stdlib-jdk8"))
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
//    implementation("io.ktor:ktor-server-core:$ktorVersion")
//    implementation("io.ktor:ktor-server-netty:$ktorVersion")
//    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
//    implementation("io.ktor:ktor-server-auth:$ktorVersion")
//    implementation("org.jetbrains.xodus:xodus-openAPI:2.0.1")
//    implementation("org.jetbrains.xodus:xodus-environment:2.0.1")
//    implementation("org.jetbrains.xodus:xodus-entity-store:2.0.1")
//    implementation("org.apache.logging.log4j:log4j-core:2.17.2")
//    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.17.2")
    testImplementation(project(":common"))
    testImplementation(project(":client"))
    testImplementation(project(":server"))

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-kotlinx-cbor:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-client-auth:$ktorVersion")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}
