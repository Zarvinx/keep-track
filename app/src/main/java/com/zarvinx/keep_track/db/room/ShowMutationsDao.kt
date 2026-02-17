package com.zarvinx.keep_track.db.room

import androidx.room.Dao
import androidx.room.ColumnInfo
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Update

data class ShowStarredUpdate(
    @ColumnInfo(name = "_id")
    val id: Int,
    @ColumnInfo(name = "starred")
    val starred: Int
)

data class ShowArchivedUpdate(
    @ColumnInfo(name = "_id")
    val id: Int,
    @ColumnInfo(name = "archived")
    val archived: Int
)

data class ShowDeleteRef(
    @ColumnInfo(name = "_id")
    val id: Int
)

/**
 * DAO for show mutation statements used by writer classes.
 */
@Dao
interface ShowMutationsDao {
    /**
     * Sets the starred flag for one show.
     *
     * @return number of updated rows.
     */
    @Update(entity = ShowEntity::class)
    fun updateStarred(update: ShowStarredUpdate): Int

    /**
     * Sets the archived flag for one show.
     *
     * @return number of updated rows.
     */
    @Update(entity = ShowEntity::class)
    fun updateArchived(update: ShowArchivedUpdate): Int

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
    @Delete(entity = ShowEntity::class)
    fun deleteShowById(show: ShowDeleteRef): Int
}
