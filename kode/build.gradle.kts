plugins {
    java
    id("com.gradleup.shadow")
}

group = "dev.ledge"
version = "1.0.0"

val kotlinRuntimeVersion = providers.gradleProperty("kotlinRuntimeVersion").get()
val coroutinesVersion = providers.gradleProperty("coroutinesVersion").get()
val serializationVersion = providers.gradleProperty("serializationVersion").get()

dependencies {
    compileOnly(project(":glap-api"))
    runtimeOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinRuntimeVersion")
    runtimeOnly("org.jetbrains.kotlin:kotlin-reflect:$kotlinRuntimeVersion")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$serializationVersion")
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$serializationVersion")
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.shadowJar {
    archiveBaseName.set("kode")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
