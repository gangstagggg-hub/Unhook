import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Nøkkelen som signerer release-bygget kommer fra miljøvariabler. GitHub Actions setter dem fra hemmelighetene i repoet.
// Uten dem bygges det bare usignerte og debug-versjoner.
val keystorePath: String? = System.getenv("UNHOOK_KEYSTORE_FILE")

android {
    // namespace er kodepakken (R-klassen). Den er uavhengig av applicationId, som er appens identitet på Google Play.
    namespace = "com.unhook.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.gangstagggg.unhook"
        minSdk = 26
        targetSdk = 36
        // Play krever et høyere versjonsnummer for hver opplasting. GitHub Actions teller det opp automatisk.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(checkNotNull(keystorePath))
                storePassword = System.getenv("UNHOOK_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("UNHOOK_KEY_ALIAS")
                keyPassword = System.getenv("UNHOOK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }

    // Web-appen i /docs er både GitHub Pages-siden og brukergrensesnittet i Android-appen.
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "../../docs")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
}
