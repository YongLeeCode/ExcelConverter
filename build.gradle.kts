plugins {
    id("java")
    id("application")
}

group = "org.example"
version = "1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass.set("org.example.Main")
}

repositories {
    mavenCentral()
}

dependencies {
    // Apache POI for Excel processing
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    // Jackson for JSON config parsing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")

    // OpenCSV for CSV writing
    implementation("com.opencsv:opencsv:5.9")

    // exp4j for formula calculation
    implementation("net.objecthunter:exp4j:0.4.8")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Fat JAR 생성 (모든 의존성 포함)
tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.example.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveBaseName.set("ExcelConverter")
}

// jpackage로 exe/dmg 생성
tasks.register<Exec>("jpackage") {
    dependsOn("jar")

    val jarFile = layout.buildDirectory.file("libs/ExcelConverter-${version}.jar").get().asFile
    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile
    val inputDir = layout.buildDirectory.dir("jpackage-input").get().asFile

    doFirst {
        outputDir.mkdirs()
        inputDir.mkdirs()

        // JAR 파일 복사
        jarFile.copyTo(File(inputDir, jarFile.name), overwrite = true)

        // profiles 폴더 복사
        val profilesSrc = file("profiles")
        val profilesDest = File(inputDir, "profiles")
        if (profilesSrc.exists()) {
            profilesSrc.copyRecursively(profilesDest, overwrite = true)
        }
    }

    commandLine(
        "jpackage",
        "--input", inputDir.absolutePath,
        "--main-jar", jarFile.name,
        "--name", "ExcelConverter",
        "--app-version", version.toString(),
        "--vendor", "Musinsa",
        "--description", "Data File Converter",
        "--dest", outputDir.absolutePath,
        "--type", if (System.getProperty("os.name").lowercase().contains("win")) "exe" else "dmg"
    )
}

// 배포 패키지 생성
tasks.register<Zip>("distPackage") {
    dependsOn("jar")

    archiveBaseName.set("ExcelConverter-dist")
    destinationDirectory.set(layout.buildDirectory.dir("dist"))

    // JAR 파일
    from(layout.buildDirectory.dir("libs")) {
        include("ExcelConverter-*.jar")
    }

    // profiles 폴더
    from("profiles") {
        into("profiles")
    }

    // 실행 스크립트
    from(".") {
        include("run.bat")
        include("run.sh")
    }
}
