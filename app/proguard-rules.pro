# ===== WNLZ Plus hardening =====
-ignorewarnings
-dontwarn **
-dontnote **

# strong name obfuscation
-repackageclasses ''
-allowaccessmodification
-obfuscationdictionary 'obfuscation-dictionary.txt'
-classobfuscationdictionary 'obfuscation-dictionary.txt'
-packageobfuscationdictionary 'obfuscation-dictionary.txt'

# strip source info
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# keep the MIUI component library (inflated from XML by name)
-keep class com.hchen.himiuix.** { *; }
-keep class * extends com.hchen.himiuix.fragment.Fragment
-keep class * extends com.hchen.himiuix.fragment.PreferenceFragmentCompat

# keep custom views referenced from resources
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# keep fragments created by the framework
-keep class * extends androidx.fragment.app.Fragment { *; }

# keep entry points
-keep class com.wunelezi.plus.MainActivity { *; }
