# Keep Room entities/DAOs
-keep class com.netsentinel.app.data.db.** { *; }

# Keep VpnService subclass accessible to the platform
-keep class com.netsentinel.app.capture.PacketCaptureVpnService { *; }

# Kotlin coroutines / metadata
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn kotlinx.coroutines.**
