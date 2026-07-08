-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class com.aether.renderer.** { *; }

-keep class * extends android.accessibilityservice.AccessibilityService { *; }
