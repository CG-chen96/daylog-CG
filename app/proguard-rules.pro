# Room
-keep class androidx.room.** { *; }
-keepclassmembers class com.dailybook.app.data.** { *; }

# 保留实体字段名，保证加密备份的 JSON 键名稳定
-keepclassmembers class * {
    @androidx.room.ColumnInfo <fields>;
}
