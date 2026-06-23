package com.example.screenshotrouter.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.settingsDataStore by preferencesDataStore(name = "settings")
val Context.localLogDataStore by preferencesDataStore(name = "local_log")
