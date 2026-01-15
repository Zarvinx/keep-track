/*
 * Copyright (C) 2012-2015 Jamie Nicol <jamie@thenicols.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.redcoracle.episodes

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.redcoracle.episodes.ui.ShowDetailScreen
import com.redcoracle.episodes.ui.theme.EpisodesTheme

class ShowActivity : AppCompatActivity() {
    
    private var showId: Int = -1
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        showId = intent.getIntExtra("showId", -1)
        if (showId == -1) {
            throw IllegalArgumentException("must provide valid showId")
        }
        
        setContent {
            EpisodesTheme {
                ShowDetailScreen(
                    showId = showId,
                    onNavigateBack = { finish() },
                    onSeasonSelected = { seasonNumber -> onSeasonSelected(seasonNumber) }
                )
            }
        }
    }
    
    private fun onSeasonSelected(seasonNumber: Int) {
        val intent = Intent(this, SeasonActivity::class.java)
        intent.putExtra("showId", showId)
        intent.putExtra("seasonNumber", seasonNumber)
        startActivity(intent)
    }
}
