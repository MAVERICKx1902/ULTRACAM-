# ULTRACAM proguard rules.
# CameraX and Compose ship consumer rules; nothing extra is required for the
# debug builds. Keep Camera2 interop keys if minification is ever enabled.
-keep class androidx.camera.camera2.interop.** { *; }
-keep class androidx.camera.core.** { *; }
