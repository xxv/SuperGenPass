# The previous build referenced a proguard-rules.txt that was never committed,
# so this file starts from what the app actually needs with R8 enabled.

# Keep the line numbers in stack traces useful while hiding the source name.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Activities, the ContentProvider and custom Views referenced from XML are kept
# automatically by the rules AGP generates from the manifest and resources, so
# nothing extra is needed for the app's own classes.
