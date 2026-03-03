# Keep main application and activity
-keep class com.offchat.android.OffChatApplication
-keep class com.offchat.android.MainActivity

# Keep Google Nearby Connections
-keep class com.google.android.gms.nearby.** { *; }

# Keep SQLDelight generated classes
-keep class com.offchat.db.** { *; }

# Keep Koin
-keep class org.koin.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** serializer(...);
    <fields>;
}
