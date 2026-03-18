# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in Android SDK tools.

# Keep data classes for Room
-keep class com.music.revive.data.local.entity.** { *; }
-keep class com.music.revive.domain.model.** { *; }

# Keep ExoPlayer classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**
