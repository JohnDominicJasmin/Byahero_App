package com.example.commutingapp.data.db

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Dao

@Dao
interface Dao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommute(commuter: Commuter)

    @Delete
    suspend fun deleteCommute(commuter: Commuter)



@Query("""
SELECT * FROM commute_table
ORDER BY 
 CASE WHEN :column = 'TIMESTAMP' THEN timestamp END DESC,
 CASE WHEN :column = 'TIME_IN_MILLIS' THEN timeInMillis END DESC,
 CASE WHEN :column = 'AVERAGE_SPEED' THEN averageSpeed_KmH END DESC,
 CASE WHEN :column = 'DISTANCE' THEN distanceInMeters END DESC,
 CASE WHEN :column = 'PLACES' THEN wentPlaces END DESC
""")
suspend fun filterBy(column:CommuterColumn):LiveData<List<Commuter>>



    @Query("SELECT SUM(timeInMillis) FROM commute_table")
    fun getTotalTimeInMillis():LiveData<Long>

    @Query("SELECT AVG(averageSpeed_KmH) FROM commute_table")
    fun getTotalAverageSpeed():LiveData<Float>

    @Query("SELECT SUM(distanceInMeters)FROM commute_table")
    fun getTotalDistance():LiveData<Int>
}