# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in D:\Delveloper\android-sdk-windows/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class com.qu.preview.** { *; }
-keep class com.qu.mp4saver.** { *; }
-keep class com.duanqu.transcode.** { *; }
-keep class com.duanqu.qupai.render.** { *; }
-keep class com.duanqu.qupai.player.** { *; }
-keep class com.duanqu.qupai.audio.** { *; }
-keep class com.aliyun.qupai.encoder.** { *; }
-keep class com.sensetime.stmobile.** { *; }
-keep class com.duanqu.qupai.yunos.** { *; }
-keep class com.aliyun.common.** { *; }
-keep class com.aliyun.jasonparse.** { *; }
-keep class com.aliyun.struct.** { *; }
-keep class com.aliyun.recorder.AliyunRecorderCreator { *; }
-keep class com.aliyun.recorder.supply.** { *; }
-keep class com.aliyun.querrorcode.** { *; }
-keep class com.qu.preview.callback.** { *; }
-keep class com.aliyun.qupaiokhttp.** { *; }
-keep class com.aliyun.crop.AliyunCropCreator { *; }
-keep class com.aliyun.crop.struct.CropParam { *; }
-keep class com.aliyun.crop.supply.** { *; }
-keep class com.aliyun.qupai.editor.pplayer.AnimPlayerView { *; }
-keep class com.aliyun.qupai.editor.impl.AliyunEditorFactory { *; }
-keep interface com.aliyun.qupai.editor.** { *; }
-keep interface com.aliyun.qupai.import_core.AliyunIImport { *; }
-keep class com.aliyun.qupai.import_core.AliyunImportCreator { *; }
-keep class com.aliyun.qupai.encoder.** { *; }
-keep class com.aliyun.leaktracer.** { *;}
-keep class **.R$* { *; }
