/*
 * Copyright (C) 2013-2014 Jamie Nicol <jamie@thenicols.net>
 * Copyright (C) 2026 Zarvinx (Kotlin conversion)
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

package com.redcoracle.episodes.services

import android.app.IntentService
import android.content.Intent
import com.redcoracle.episodes.RefreshShowUtil

class RefreshShowService : IntentService(RefreshShowService::class.java.name) {
    override fun onHandleIntent(intent: Intent?) {
        val showId = intent?.getIntExtra("showId", 0) ?: 0
        RefreshShowUtil.refreshShow(showId, contentResolver)
    }
}
