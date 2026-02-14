package com.redcoracle.episodes.db.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

@Dao
@SkipQueryVerification
interface ShowMutationsDao {
    @Query("UPDATE shows SET starred = :starred WHERE _id = :showId")
    fun updateStarred(showId: Int, starred: Int): Int

    @Query("UPDATE shows SET archived = :archived WHERE _id = :showId")
    fun updateArchived(showId: Int, archived: Int): Int

    @Query("DELETE FROM episodes WHERE show_id = :showId")
    fun deleteEpisodesByShowId(showId: Int): Int

    @Query("DELETE FROM shows WHERE _id = :showId")
    fun deleteShowById(showId: Int): Int
}
