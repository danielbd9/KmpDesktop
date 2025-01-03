import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose)
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            resources.srcDir("src/main/resources")
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation("com.alphacephei:vosk:0.3.38")
            implementation("com.googlecode.json-simple:json-simple:1.1.1")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)

            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.4")

           // implementation(libs.voskAndroid)
            implementation("com.alphacephei:vosk:0.3.38")
            implementation("com.googlecode.json-simple:json-simple:1.1.1")
            implementation("com.google.code.gson:gson:2.10.1")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
            implementation("com.googlecode.json-simple:json-simple:1.1.1") // Para processar JSON
            implementation("com.squareup.okhttp3:okhttp:4.10.0") // Para fazer requisições HTTP para tradução
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4") // Para usar corrotinas
        }

    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Desktop"
            packageVersion = "1.0.0"

            linux {
                iconFile.set(project.file("desktopAppIcons/LinuxIcon.png"))
            }
            windows {
                iconFile.set(project.file("desktopAppIcons/WindowsIcon.ico"))
            }
            macOS {
                iconFile.set(project.file("desktopAppIcons/MacosIcon.icns"))
                bundleID = "org.company.app.desktopApp"
            }
        }
    }
}
