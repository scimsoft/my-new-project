# TapShare ProGuard Rules

# Keep NFC-related classes
-keep class com.tapshare.app.nfc.** { *; }
-keep class com.tapshare.app.transfer.** { *; }

# Keep model classes for serialization
-keep class com.tapshare.app.model.** { *; }

# Compose
-dontwarn androidx.compose.**
