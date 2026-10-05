plugins {
    kotlin("jvm") version "2.1.21"
    application
}



val appSrc = "../../app/src"

sourceSets {
    main { kotlin.srcDirs("src/main/kotlin", "$appSrc/main/java/com/noor/wallpapers/art") }
    test { kotlin.srcDirs("$appSrc/test/java/com/noor/wallpapers/art") }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("com.noor.wallpapers.preview.PreviewKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
    args = listOf(
        project.findProperty("out")?.toString() ?: "build/previews",
        project.findProperty("scale")?.toString() ?: "0.5",
        project.findProperty("only")?.toString() ?: "",
        project.findProperty("device")?.toString() ?: "all",
    )
}
