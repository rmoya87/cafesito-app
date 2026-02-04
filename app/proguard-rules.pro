# Credential Manager
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Mantener nombres de métodos para la interoperabilidad con servicios de Google
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}

# Prevención de eliminación de recursos necesarios por ID de Google
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.ApiException { *; }

# ✅ SOLUCIÓN AL ERROR DE SLF4J (Ktor / Supabase)
# Ignorar advertencias de clases faltantes de SLF4J que no se usan en Android
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
