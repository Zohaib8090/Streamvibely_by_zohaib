# NewPipe Extractor bundles Mozilla Rhino, which references desktop-only classes that do not exist on Android.
-dontwarn java.beans.**
-dontwarn javax.script.**
-dontwarn jdk.dynalink.**
-dontwarn org.mozilla.javascript.tools.**
