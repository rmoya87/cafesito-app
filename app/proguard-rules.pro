# --- REGLAS BASE ---
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**

# --- KOTLIN SERIALIZATION (Crítico para Supabase) ---
# Mantiene los serializadores generados y evita que se eliminen campos de tus modelos
-keepclassmembers class com.cafesito.app.data.** {
    *** Companion;
    *** $serializer;
}
-keep,allowobfuscation,allowoptimization class com.cafesito.app.data.** { *; }
-keep class kotlinx.serialization.json.** { *; }
-dontwarn kotlinx.serialization.**

# --- SUPABASE & KTOR ---
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-dontwarn io.github.jan.supabase.**

# --- SLF4J ---
-dontwarn org.slf4j.**

# --- HILT / DAGGER ---
-keep class dagger.hilt.android.internal.** { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.ComponentEntryPoint { *; }

# --- ROOM ---
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.**

# --- COIL (Carga de imágenes) ---
-keep class coil.** { *; }
-dontwarn coil.**

# --- OPTIMIZACIÓN AGRESIVA ---
-repackageclasses ''
-allowaccessmodification
-mergeinterfacesaggressively
