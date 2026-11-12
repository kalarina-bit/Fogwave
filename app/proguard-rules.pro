-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Debug and verbose logging is stripped from release builds.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
