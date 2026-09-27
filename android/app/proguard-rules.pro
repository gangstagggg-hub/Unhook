# Behold metodene som web-appen kaller via window.Android
-keepclassmembers class com.unhook.app.Bridge {
    @android.webkit.JavascriptInterface <methods>;
}
