# JavaScript bridge methods used by the embedded YouTube player page.
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
