# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in Android SDK tools.

# Keep data classes for Room
-keep class com.music.revive.data.local.entity.** { *; }
-keep class com.music.revive.domain.model.** { *; }

# Keep ExoPlayer classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Hilt rules
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep,allowobfuscation,allowshrinking class com.music.revive.di.** { *; }
-keep,allowobfuscation,allowshrinking class com.music.revive.ReviveApplication { *; }
-keep,allowobfuscation,allowshrinking class com.music.revive.MainActivity { *; }
-keep,allowobfuscation,allowshrinking class com.music.revive.service.** { *; }
-keep,allowobfuscation,allowshrinking class com.music.revive.data.repository.** { *; }
-keep,allowobfuscation,allowshrinking class com.music.revive.data.datasource.** { *; }

# Keep Hilt generated classes
-keep class **_HiltComponents { *; }
-keep class **_HiltModules { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.music.revive.presentation.navigation.**$$serializer { *; }
-keepclassmembers class com.music.revive.presentation.navigation.** {
    *** Companion;
}
-keepclasseswithmembers class com.music.revive.presentation.navigation.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Coil
-keep class coil.** { *; }
-keep interface coil.** { *; }
-keep class * extends coil.ImageLoader { *; }

# Keep all Compose related classes
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep all ViewModels
-keep class com.music.revive.presentation.screen.**ViewModel { *; }
-keep class com.music.revive.presentation.navigation.SharedMusicViewModel { *; }