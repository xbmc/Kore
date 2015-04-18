# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /opt/android-sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# Don't obfuscate for now. Obfuscation decreases apk size by about 300k
-dontobfuscate

# Picasso
-dontwarn com.squareup.okhttp.**

# okio via OkHttp
-dontwarn okio.**

# Butterknife
-dontwarn butterknife.internal.**
-keep class **$$ViewInjector { *; }
-keepnames class * { @butterknife.InjectView *;}

# Jackson
-dontskipnonpubliclibraryclassmembers
-keepattributes EnclosingMethod, Signature
#-keep class org.codehaus.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**

# EventBus
-keepclassmembers class ** {
    public void onEvent*(**);
}

# SearchView
-keep class android.support.v7.widget.SearchView { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
