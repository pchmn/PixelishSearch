# Ktor utilise SLF4J en dépendance optionnelle, on n'en a pas besoin sur Android.
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# Ktor: garde les engines découverts via ServiceLoader (sinon plus de réseau en release).
-keep class io.ktor.client.engine.android.** { *; }
-keep class * implements io.ktor.client.HttpClientEngineContainer { *; }

# kotlinx.serialization: garde les serializers générés.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.pchmn.pixelishsearch.**$$serializer { *; }
-keepclassmembers class com.pchmn.pixelishsearch.** {
    *** Companion;
}
-keepclasseswithmembers class com.pchmn.pixelishsearch.** {
    kotlinx.serialization.KSerializer serializer(...);
}
