# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

###############################################
# GIỮ TÊN CLASS & FUNCTION (KHÔNG OBFUSCATE TÊN)
###############################################
-dontobfuscate
-dontusemixedcaseclassnames

###############################################
# TỐI ƯU CODE TỐI ĐA (LÀM KHÓ ĐỌC BYTECODE)
###############################################
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# Bật tất cả optimization (trừ một số nguy hiểm)
-optimizations !code/simplification/cast,!code/simplification/advanced,!field/*,!class/merging/*

###############################################
# XÓA DEBUG INFO (QUAN TRỌNG)
###############################################
-renamesourcefileattribute Hidden
-keepattributes Exceptions
# KHÔNG giữ SourceFile, LineNumberTable → Khó debug reverse

###############################################
# GIỮ CLASS SDK (KHÔNG GIỮ MEMBERS)
###############################################
-keep,allowobfuscation class com.ads.nkh.**
-keep interface com.ads.nkh.** { *; }

###############################################
# THIRD-PARTY SDK
###############################################
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**
-dontwarn com.adjust.**
-dontwarn com.appsflyer.**
-dontwarn com.bytedance.sdk.openadsdk.**
-dontwarn com.mbridge.**

-keep class com.google.android.gms.ads.identifier.** { *; }
-keep class com.adjust.sdk.** { *; }
-keep class com.appsflyer.** { *; }
-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.mbridge.** { *; }

###############################################
# IGNORE WARNINGS
###############################################
-dontnote
-ignorewarnings


