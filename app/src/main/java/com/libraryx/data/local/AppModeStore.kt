package com.libraryx.data.local

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.libraryx.data.model.AppMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appModeDataStore by preferencesDataStore(name = "studylab_app_mode")

/**
 * Mirrors src/lib/appMode.ts (`getAppMode`/`setAppMode`/`clearAppMode`), backed by
 * Jetpack DataStore instead of `window.localStorage`.
 */
@Singleton
class AppModeStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val MODE = stringPreferencesKey("studylab_app_mode")
    }

    val appMode: Flow<AppMode?> = context.appModeDataStore.data.map { prefs ->
        when (prefs[Keys.MODE]) {
            "solo" -> AppMode.Solo
            "saas" -> AppMode.Saas
            else -> null
        }
    }

    suspend fun getAppModeOnce(): AppMode? = appMode.first()

    suspend fun setAppMode(mode: AppMode) {
        context.appModeDataStore.edit { prefs ->
            prefs[Keys.MODE] = if (mode == AppMode.Solo) "solo" else "saas"
        }
    }

    suspend fun clearAppMode() {
        context.appModeDataStore.edit { prefs -> prefs.remove(Keys.MODE) }
    }
}
