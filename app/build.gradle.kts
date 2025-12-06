plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.synapseguard.vpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.synapseguard.vpn"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        val releaseStoreFile = providers.gradleProperty("releaseStoreFile")
            .orElse(providers.environmentVariable("RELEASE_STORE_FILE"))
        val releaseStorePassword = providers.gradleProperty("releaseStorePassword")
            .orElse(providers.environmentVariable("RELEASE_STORE_PASSWORD"))
        val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias")
            .orElse(providers.environmentVariable("RELEASE_KEY_ALIAS"))
        val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword")
            .orElse(providers.environmentVariable("RELEASE_KEY_PASSWORD"))
        val runningReleaseTask = gradle.startParameter.taskNames.any {
            it.contains("Release", ignoreCase = true)
        }

        val releaseProperties = mapOf(
            "releaseStoreFile" to releaseStoreFile,
            "releaseStorePassword" to releaseStorePassword,
            "releaseKeyAlias" to releaseKeyAlias,
            "releaseKeyPassword" to releaseKeyPassword
        )

        val releaseKeystoreFile = releaseStoreFile.orNull
            ?.takeIf { it.isNotBlank() }
            ?.let { file(it) }

        create("release") {
            val missingKeys = releaseProperties
                .filterValues { !it.isPresent || it.get().isBlank() }
                .keys
            val hasValidReleaseKeystore = missingKeys.isEmpty() &&
                releaseKeystoreFile?.exists() == true

            if (hasValidReleaseKeystore) {
                storeFile = releaseKeystoreFile
                storePassword = releaseStorePassword.get()
                keyAlias = releaseKeyAlias.get()
                keyPassword = releaseKeyPassword.get()
            } else {
                initWith(signingConfigs.getByName("debug"))
                if (runningReleaseTask) {
                    val reasons = buildList {
                        if (missingKeys.isNotEmpty()) {
                            add("Missing properties: ${missingKeys.joinToString()}")
                        }
                        val keystorePath = releaseKeystoreFile?.path ?: releaseStoreFile.orNull
                        if (keystorePath.isNullOrBlank() || releaseKeystoreFile?.exists() != true) {
                            add("Keystore not found at ${keystorePath?.ifBlank { "<unset>" } ?: "<unset>"}")
                        }
                    }.joinToString("; ")

                    throw GradleException(
                        "Release signing config is missing or invalid ($reasons). Provide releaseStoreFile/releaseStorePassword/releaseKeyAlias/releaseKeyPassword in gradle.properties or CI environment variables and ensure the keystore file exists."
                    )
                } else {
                    logger.warn(
                        "Release signing config not provided; using debug signing for non-release tasks. Missing: ${missingKeys.joinToString()}"
                    )
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":vpn-service"))

    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Accompanist (for system UI controller, permissions)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Timber for logging
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:6.1.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
