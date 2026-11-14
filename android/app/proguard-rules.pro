# Hesabyar release rules
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod

# kotlinx.serialization
-keepclassmembers class ir.hesabyar.core.database.entity.** { *; }
-keepclassmembers class ir.hesabyar.data.backup.** { *; }
-keepattributes RuntimeVisibleAnnotations

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Hilt / Dagger
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
