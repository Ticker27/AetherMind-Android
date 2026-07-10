# Keep JNI bridge method names stable.
-keep class com.aethermind.execution.AetherExecutionNative { *; }

# AccessibilityService is referenced from AndroidManifest.xml.
-keep class com.aethermind.execution.AetherAccessibilityService { *; }
