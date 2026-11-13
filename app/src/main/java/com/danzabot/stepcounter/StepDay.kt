package com.danzabot.stepcounter

import androidx.room.*

@Entity(tableName = "step_days")
data class StepDay(
    @PrimaryKey val date: String,   // "2026-11-13"
    val steps: Int,
    val miles: Double,
    val calories: Double
)

@Dao
interface StepDayDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(day: StepDay)

    @Query("SELECT * FROM step_days WHERE date = :date")
    fun get(date: String): StepDay?

    @Query("SELECT * FROM step_days ORDER BY date DESC LIMIT :limit")
    fun recent(limit: Int): List<StepDay>

    @Query("SELECT * FROM step_days WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    fun range(from: String, to: String): List<StepDay>

    @Query("SELECT SUM(steps) FROM step_days WHERE date BETWEEN :from AND :to")
    fun totalSteps(from: String, to: String): Int?

    @Query("SELECT COUNT(*) FROM step_days")
    fun count(): Int
}

@Database(entities = [StepDay::class], version = 1, exportSchema = false)
abstract class StepDatabase : RoomDatabase() {
    abstract fun stepDayDao(): StepDayDao

    companion object {
        @Volatile private var instance: StepDatabase? = null

        fun get(ctx: android.content.Context): StepDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    ctx.applicationContext, StepDatabase::class.java, "steps.db"
                ).allowMainThreadQueries().build().also { instance = it }
            }
    }
}
