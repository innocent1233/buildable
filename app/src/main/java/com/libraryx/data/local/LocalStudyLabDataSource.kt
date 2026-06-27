package com.libraryx.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.Student
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.studyLabDataStore by preferencesDataStore(name = "studylab_local_store")

/**
 * Mirrors the localStorage-backed persistence functions in src/lib/store.ts
 * (getStudents/saveStudents/getSettings/saveSettings), using the *same* storage
 * keys ("studylab_students", "studylab_settings") so a JSON backup exported by
 * the original web app round-trips through [BackupSerializer] unchanged.
 */
@Singleton
class LocalStudyLabDataSource @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val STUDENTS = stringPreferencesKey("studylab_students")
        val SETTINGS = stringPreferencesKey("studylab_settings")
    }

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val studentsFlow: Flow<List<Student>> = context.studyLabDataStore.data.map { prefs ->
        prefs[Keys.STUDENTS]?.let { raw ->
            runCatching { json.decodeFromString<List<Student>>(raw) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    val settingsFlow: Flow<AppSettings> = context.studyLabDataStore.data.map { prefs ->
        prefs[Keys.SETTINGS]?.let { raw ->
            runCatching { json.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings.Default)
        } ?: AppSettings.Default
    }

    suspend fun getStudentsOnce(): List<Student> = studentsFlow.first()
    suspend fun getSettingsOnce(): AppSettings = settingsFlow.first()

    suspend fun saveStudents(students: List<Student>) {
        context.studyLabDataStore.edit { prefs -> prefs[Keys.STUDENTS] = json.encodeToString(students) }
    }

    suspend fun saveSettings(settings: AppSettings) {
        context.studyLabDataStore.edit { prefs -> prefs[Keys.SETTINGS] = json.encodeToString(settings) }
    }
}
