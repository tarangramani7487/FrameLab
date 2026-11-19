
# WorkManager's Room database is created by reflection (no-arg constructor); R8 full mode strips it.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class androidx.work.impl.WorkDatabase_Impl { *; }
