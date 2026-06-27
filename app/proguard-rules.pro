# Add project specific ProGuard rules here.

# Keep data model classes used for Firestore (de)serialization via reflection,
# mirroring the plain TS interfaces in lib/store.ts and lib/saas/types.ts.
-keepclassmembers class com.libraryx.data.model.** {
    <init>(...);
    <fields>;
}
-keep class com.libraryx.data.model.** { *; }

# Firebase Firestore POJO mapping
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.firebase.firestore.** { *; }
