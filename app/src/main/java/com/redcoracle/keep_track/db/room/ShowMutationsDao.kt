package com.redcoracle.keep_track.db.room

import androidx.room.Dao
import androidx.room.Query
import androidx.room.SkipQueryVerification

/**
 * DAO for show mutation statements used by writer classes.
 */
@Dao
@SkipQueryVerification
interface ShowMutationsDao {
    /**
     * Sets the starred flag for one show.
     *
     * @return number of updated rows.
     */
    @Query("UPDATE shows SET starred = :starred WHERE _id = :showId")
    fun updateStarred(showId: Int, starred: Int): Int

    /**
     * Sets the archived flag for one show.
     *
     * @return number of updated rows.
     */
    @Query("UPDATE shows SET archived = :archived WHERE _id = :showId")
    fun updateArchived(showId: Int, archived: Int): Int

    /**
     * Deletes all episodes for a show.
     *
     * @return number of deleted episode rows.
     */
    @Query("DELETE FROM episodes WHERE show_id = :showId")
    fun deleteEpisodesByShowId(showId: Int): Int

    /**
     * Deletes one show row by id.
     *
     * @return number of deleted show rows.
     */
    @Query("DELETE FROM shows WHERE _id = :showId")
    fun deleteShowById(showId: Int): Int
}
