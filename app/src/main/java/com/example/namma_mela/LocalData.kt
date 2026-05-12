package com.example.namma_mela

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String,
    val password: String,
    // NEW COLOR: Vibrant Emerald Green (0xFF2ECC71) - High visibility
    val themeColor: Int = 0xFF2ECC71.toInt(),
    val preferredLanguage: String = "English",
    val themeMode: String = "System"
)

@Entity(tableName = "bookings")
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val seatNumber: Int,
    val playName: String
)

@Entity(tableName = "fan_wall")
data class FanWallComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val author: String,
    val comment: String
)

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun registerUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUser(username: String): User?

    @Update
    suspend fun updateUser(user: User)

    @Insert
    suspend fun addBooking(booking: Booking)

    @Query("SELECT * FROM bookings")
    fun getAllBookings(): Flow<List<Booking>>

    @Query("SELECT * FROM bookings WHERE username = :username")
    fun getHistory(username: String): Flow<List<Booking>>

    @Delete
    suspend fun deleteBooking(booking: Booking)

    @Insert
    suspend fun addComment(comment: FanWallComment)

    @Query("SELECT * FROM fan_wall ORDER BY id DESC")
    fun getComments(): Flow<List<FanWallComment>>
}

@Database(entities = [User::class, Booking::class, FanWallComment::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "namma_mela_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}