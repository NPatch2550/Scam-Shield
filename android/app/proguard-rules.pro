# ProGuard rules for Scam Shield Android Edition
-keep class com.scamshield.app.network.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
