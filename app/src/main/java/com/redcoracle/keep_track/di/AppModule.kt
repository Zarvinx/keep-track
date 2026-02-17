package com.redcoracle.keep_track.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.redcoracle.keep_track.db.room.AppDatabase
import com.redcoracle.keep_track.db.room.AppReadDao
import com.redcoracle.keep_track.db.room.ShowQueriesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides app-scoped persistence and preferences dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /**
     * Provides singleton Room database instance bound to app DB file.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    /**
     * Provides read-model DAO used by list/detail view models.
     */
    @Provides
    fun provideAppReadDao(database: AppDatabase): AppReadDao = database.appReadDao()

    /**
     * Provides show-focused query DAO for show detail screens.
     */
    @Provides
    fun provideShowQueriesDao(database: AppDatabase): ShowQueriesDao = database.showQueriesDao()

    /**
     * Provides default shared preferences.
     */
    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
}
