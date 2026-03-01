-keep class com.georgernstgraf.aitranscribe.data.local.** { *; }
-keep class com.georgernstgraf.aitranscribe.domain.model.** { *; }

-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

-dontwarn javax.annotation.**
-dontwarn javax.inject.**

-keepclassmembers class * {
    @dagger.hilt.android.scopes.ActivityScoped public *** provide*();
}

-keepclassmembers class * {
    @dagger.hilt.android.scopes.FragmentScoped public *** provide*();
}

-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.arthenica.ffmpegkit.** { *; }
-dontwarn com.arthenica.ffmpegkit.**