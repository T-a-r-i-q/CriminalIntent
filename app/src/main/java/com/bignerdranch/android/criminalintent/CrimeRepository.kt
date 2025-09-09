package com.bignerdranch.android.criminalintent

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID


private const val DATABASE_NAME = "crime-database"

class CrimeRepository private constructor(context: Context, private val coroutineScope: CoroutineScope = GlobalScope) {
    private val database: CrimeDatabase= Room.databaseBuilder(
        context.applicationContext, CrimeDatabase::class.java, DATABASE_NAME
        ).createFromAsset(DATABASE_NAME).build()

    fun getCrimes(): Flow<List<Crime>> = database.crimeDAO().getCrimes()
    suspend fun getCrime(id: UUID): Crime = database.crimeDAO().getCrime(id)
    //crimeDAO was listed Dao in text, this created error hence DAO
    fun updateCrime(crime: Crime) {
        coroutineScope.launch {
            database.crimeDAO().updateCrime(crime)
        }
    }


    companion object {
        private var INSTANCE: CrimeRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = CrimeRepository(context)
            }
        }
        fun get(): CrimeRepository {
            return INSTANCE?:
            throw IllegalArgumentException("CrimeRepository must be initialized")
        }
    }

}