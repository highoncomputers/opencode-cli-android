# OpenCode Android App ProGuard Rules

# Keep application class
-keep class ai.opencode.android.OpenCodeApplication { *; }

# Keep all activities
-keep class ai.opencode.android.** { *; }

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep JSON
-keep class org.json.** { *; }
