package com.dailybook.app.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context

@Database(
    entities = [Routine::class, DayTask::class, RoutineCheck::class, Trip::class, TripItem::class],
    version = 4,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun dao(): DiaryDao

    abstract fun tripDao(): TripDao

    companion object {
        private const val DB_NAME = "daylog.db"

        /** v2：事务支持记录金额（收入 / 支出），单位分；公共事务可按天覆盖 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine ADD COLUMN amount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE routine ADD COLUMN amountKind INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE day_task ADD COLUMN amount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE day_task ADD COLUMN amountKind INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE routine_check ADD COLUMN amount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE routine_check ADD COLUMN amountKind INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** v3：新增独立的出游模块（trip / trip_item），与每日事务无关 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trip` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`destination` TEXT NOT NULL, " +
                        "`startDate` TEXT NOT NULL, " +
                        "`endDate` TEXT NOT NULL, " +
                        "`note` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trip_item` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`tripId` INTEGER NOT NULL, " +
                        "`date` TEXT NOT NULL, " +
                        "`time` TEXT NOT NULL, " +
                        "`kind` INTEGER NOT NULL, " +
                        "`title` TEXT NOT NULL, " +
                        "`detail` TEXT NOT NULL, " +
                        "`amount` INTEGER NOT NULL, " +
                        "`sortOrder` INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_item_tripId` ON `trip_item` (`tripId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_trip_item_date` ON `trip_item` (`date`)")
            }
        }

        /**
         * v4：公共事务支持「从某天起不再出现」。
         * 删除只把 endDate 定到昨天并打上 deleted，历史日期照旧按规则回溯，
         * 老版本归档的事务也补上截止日，避免升级后突然在清单里复活。
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routine ADD COLUMN endDate TEXT")
                db.execSQL("ALTER TABLE routine ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL(
                    "UPDATE routine SET endDate = date('now', '-1 day', 'localtime') " +
                        "WHERE archived = 1 AND endDate IS NULL"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context) =
            Room.databaseBuilder(context, AppDatabase::class.java, DB_NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
