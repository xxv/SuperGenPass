import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Release signing credentials are kept out of the repository. Either drop a
// keystore.properties in the project root (see keystore.properties.example) or
// set the matching SGP_* environment variables, which is what CI does. When
// neither is present the release build still assembles, just unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun credential(key: String, environmentVariable: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(environmentVariable)

val keystorePath = credential("storeFile", "SGP_KEYSTORE_FILE")

android {
    namespace = "info.staticfree.SuperGenPass"
    compileSdk = 36

    defaultConfig {
        applicationId = "info.staticfree.SuperGenPass"
        minSdk = 21
        targetSdk = 36
        versionCode = 25
        versionName = "3.2.0"

        testApplicationId = "info.staticfree.SuperGenPass.test"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = credential("storePassword", "SGP_KEYSTORE_PASSWORD")
                keyAlias = credential("keyAlias", "SGP_KEY_ALIAS")
                keyPassword = credential("keyPassword", "SGP_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        // RememberedDomainProvider derives its authority from APPLICATION_ID.
        buildConfig = true
    }

    testOptions {
        managedDevices {
            // A project-owned emulator. Gradle creates, boots and tears this
            // down itself, so running the tests never touches whatever devices
            // or emulators happen to be plugged in or shared with other
            // projects. Run with: ./gradlew pixel6Api34DebugAndroidTest
            localDevices {
                create("pixel6Api34") {
                    device = "Pixel 6"
                    apiLevel = 34
                    // ATD images are headless and much quicker than the full
                    // Google APIs images; these tests need no Play services.
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }

    lint {
        // Keep the build honest about anything that would break on a modern
        // device, but don't fail on the long tail of style warnings.
        warningsAsErrors = false
        abortOnError = true
        checkReleaseBuilds = true
    }

    dependenciesInfo {
        // Play doesn't need the encrypted dependency blob, and leaving it out
        // keeps reproducible builds verifiable by third parties.
        includeInApk = false
        includeInBundle = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

base {
    archivesName = "${android.defaultConfig.applicationId}-${android.defaultConfig.versionCode}-${android.defaultConfig.versionName}"
}

dependencies {
    implementation(libs.androidx.annotation)
    implementation(libs.commons.codec)
    implementation(libs.zxing.android.integration)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.junit)
}
