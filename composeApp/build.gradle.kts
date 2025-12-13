import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            val koogVersion = "0.5.0"
            implementation("ai.koog:koog-agents:$koogVersion")
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation("net.sourceforge.plantuml:plantuml:1.2025.10")
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            // Force kotlinx-datetime-jvm version 0.6.2 as used by koog-agents 0.5.0
            implementation("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.2")
        }
    }
    
}

// Force resolution strategy to use 0.6.2 instead of 0.7.1
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
        force("org.jetbrains.kotlinx:kotlinx-datetime-jvm:0.6.2")
    }
}

compose.desktop {
    application {
        mainClass = "com.pawlowski.io_agents_desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.pawlowski.io_agents_desktop"
            packageVersion = "1.0.0"
        }
    }
}
