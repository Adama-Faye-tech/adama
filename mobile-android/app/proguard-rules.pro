# Aggressive obfuscation settings
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Flatten the package hierarchy to make the app structure harder to read
-repackageclasses ''
-allowaccessmodification

# Remove debug information: source file and line numbers
-renamesourcefileattribute SourceFile
-keepattributes !SourceFile,!LineNumberTable

# Also remove other sensitive attributes
-keepattributes *Annotation*,Signature,InnerClasses

# Entry points: do NOT rename classes in the manifest to avoid issues 
# unless you are sure your build tool updates the manifest.
# But keep them as entry points.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference

# Ensure parameter names are also obfuscated
-keepparameternames