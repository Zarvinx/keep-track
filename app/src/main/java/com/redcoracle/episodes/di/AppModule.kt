package com.redcoracle.episodes.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.redcoracle.episodes.db.room.AppDatabase
import com.redcoracle.episodes.db.room.AppReadDao
import com.redcoracle.episodes.db.room.ShowQueriesDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getInstance(context)

    @Provides
    fun provideAppReadDao(database: AppDatabase): AppReadDao = database.appReadDao()

    @Provides
    fun provideShowQueriesDao(database: AppDatabase): ShowQueriesDao = database.showQueriesDao()

    @Provides
    @Singleton
    fun provideSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
}
