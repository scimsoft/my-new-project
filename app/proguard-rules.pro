# TapShare ProGuard Rules

# Keep NFC-related classes
-keep class com.scimsoft.tap2share.nfc.** { *; }
-keep class com.scimsoft.tap2share.transfer.** { *; }

# Keep model classes for serialization
-keep class com.scimsoft.tap2share.model.** { *; }

# Compose
-dontwarn androidx.compose.**
