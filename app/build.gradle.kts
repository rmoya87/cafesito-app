import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

val supabaseUrl = providers.gradleProperty("SUPABASE_URL")
    .orElse("https://ubcxjmagimjhpsehqync.supabase.co")
    .get()

val supabasePublishableKey = providers.gradleProperty("SUPABASE_PUBLISHABLE_KEY")
    .orElse("sb_publishable_M2cY8wb50_I_pfnv_ZcukA_AIvnk66z")
    .get()

val googleServerClientId = providers.gradleProperty("GOOGLE_SERVER_CLIENT_ID")
    .orElse("789398399906-468mj79uf2t4e485n7ilufv4eiouk3sm.apps.googleusercontent.com")
    .get()

val passkeyRequestJson = providers.gradleProperty("PASSKEY_REQUEST_JSON")
    .orElse("")
    .get()

val mapTilerApiKey = providers.gradleProperty("MAPTILER_API_KEY").orElse("").get()
val gtmContainerId = providers.gradleProperty("GTM_CONTAINER_ID").orElse("").get()

// Cargar propiedades de firma
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.cafesito.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cafesito.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 230
        versionName = "2026.3.18"

        testInstrumentationRunner = "com.cafesito.app.HiltTestRunner"

        // Símbolos nativos para ANR y crashes en Play Console (dependencias con .so: CameraX, ML Kit, ExoPlayer, etc.)
        ndk {
            debugSymbolLevel = "SYMBOL_TABLE"
        }

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"$supabasePublishableKey\"")
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
        buildConfigField("String", "PASSKEY_REQUEST_JSON", "\"$passkeyRequestJson\"")
        buildConfigField("String", "MAPTILER_API_KEY", "\"$mapTilerApiKey\"")
        buildConfigField("String", "GTM_CONTAINER_ID", "\"$gtmContainerId\"")

        manifestPlaceholders["usesCleartextTraffic"] = "true"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystoreProperties.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
            
            isMinifyEnabled = true
            isShrinkResources = true
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }

        debug {
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("main") {
            // Compile against a generated res tree that excludes .svg files.
            res.setSrcDirs(listOf(layout.buildDirectory.dir("generated/filteredRes/main")))
        }
    }
}

val prepareFilteredRes by tasks.registering(Copy::class) {
    from(layout.projectDirectory.dir("src/main/res")) {
        exclude("**/*.svg")
    }
    into(layout.buildDirectory.dir("generated/filteredRes/main"))
}

val syncSvgIconsFromDrawable by tasks.registering(Copy::class) {
    from(layout.projectDirectory.dir("src/main/res/drawable")) {
        include("cafeina.svg", "formato.svg")
    }
    into(layout.projectDirectory.dir("src/main/assets/icons"))
}

val syncWebTechnicalIconsFromDrawable by tasks.registering(Copy::class) {
    from(layout.projectDirectory.dir("src/main/res/drawable")) {
        include(
            "pais.png",
            "especialidad.png",
            "variedad.png",
            "tueste.png",
            "proceso.png",
            "formato.png"
        )
    }
    into(layout.projectDirectory.dir("../webApp/public/android-drawable"))
}

tasks.named("preBuild") {
    dependsOn(prepareFilteredRes)
    dependsOn(syncSvgIconsFromDrawable)
    dependsOn(syncWebTechnicalIconsFromDrawable)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-parameters"
        )
    }
}

// Hilt usa kotlin-metadata-jvm; forzar 2.3.0 para compatibilidad con Kotlin 2.3
configurations.all {
    resolutionStrategy {
        force("org.jetbrains.kotlin:kotlin-metadata-jvm:2.3.0")
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // UI & Material Components
    implementation(libs.google.material)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.browser)
    implementation("androidx.biometric:biometric:1.1.0")
    
    // ML Kit & Barcode Scanning
    implementation(libs.google.play.services.code.scanner)
    implementation(libs.google.play.services.mlkit.barcode.scanning)
    implementation(libs.google.mlkit.barcode.scanning)
    
    // Accompanist Permissions
    implementation(libs.accompanist.permissions)

    // CameraX (native camera scanning)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Paging 3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Google Auth & Credentials
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.google.play.services.tagmanager)
    implementation(libs.google.play.services.tagmanager.api)
    // Crashlytics deshabilitado temporalmente: requiere plugin y puede fallar al abrir si no está configurado
    // implementation(libs.firebase.crashlytics)

    // Media3 (Video Player)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Room (DB)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Supabase & Ktor
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.storage)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Hilt (DI)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.hilt.compiler)
    ksp(libs.androidx.hilt.compiler)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)

    // OpenStreetMap (Cafés probados por país)
    implementation(libs.osmdroid.android)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
