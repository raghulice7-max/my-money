package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        FuelEntryEntity::class,
        PendingSmsTransactionEntity::class,
        InvestmentEntity::class,
        GoalEntity::class,
        RecurringReminderEntity::class,
        ReminderPaymentEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create new table for reminder_payments
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `reminder_payments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `reminderId` INTEGER NOT NULL,
                        `paidDate` TEXT NOT NULL,
                        `amount` REAL NOT NULL
                    )""".trimIndent()
                )
                // Add lastPaidDate column to recurring_reminders if missing
                try {
                    db.execSQL("ALTER TABLE `recurring_reminders` ADD COLUMN `lastPaidDate` TEXT DEFAULT NULL")
                } catch (e: Exception) {
                    // Column might already exist in fresh table creation
                }
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "finance_tracker_database"
                )
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
