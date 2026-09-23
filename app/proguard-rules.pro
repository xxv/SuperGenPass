# The previous build referenced a proguard-rules.txt that was never committed,
# so this file starts from what the app actually needs with R8 enabled.

# Keep the line numbers in stack traces useful while hiding the source name.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Activities, the ContentProvider and custom Views referenced from XML are kept
# automatically by the rules AGP generates from the manifest and resources, so
# nothing extra is needed for the app's own classes.

# commons-codec targets both the JDK and Android and touches a few classes that
# do not exist on Android. They are never reached from the code paths this app
# uses, so let R8 drop them quietly instead of warning.
-dontwarn org.apache.commons.codec.**

# The ZXing integration helper is entirely reflection-free, but it references
# the scanner app's classes by name only.
-dontwarn com.google.zxing.**
