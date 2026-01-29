plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application) apply false
    id("com.android.library")
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.sqldelight.runtime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
            }
        }
        val iosMain by creating {
            dependencies {
                implementation(libs.sqldelight.native.driver)
            }
        }
        iosMain.dependsOn(commonMain)
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        val desktopMain by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

android {
    namespace = "com.cafesito.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
}

sqldelight {
    databases {
        create("CoffeeCacheDatabase") {
            packageName.set("com.cafesito.shared.cache")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            migrationOutputDirectory.set(file("src/commonMain/sqldelight/com/cafesito/shared/cache/migrations"))
            verifyMigrations = true
        }
    }
}
