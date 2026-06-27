package com.libraryx.data.local

import com.libraryx.data.model.AppSettings
import com.libraryx.data.model.Student
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant

/**
 * Mirrors `exportBackup`/`importBackup` from src/lib/store.ts. The JSON shape
 * (`version`, `exportDate`, `students`, `settings`) is identical to the original
 * web app's export so backups are interchangeable between the two.
 */
object BackupSerializer {

    @Serializable
    data class BackupPayload(
        val version: Int = 2,
        val exportDate: String,
        val students: List<Student>,
        val settings: AppSettings
    )

    data class ImportResult(val students: List<Student>, val settings: AppSettings)

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun export(students: List<Student>, settings: AppSettings): String {
        val payload = BackupPayload(
            exportDate = Instant.now().toString(),
            students = students,
            settings = settings
        )
        return json.encodeToString(payload)
    }

    /** Mirrors `importBackup`'s lenient merge of imported settings onto defaults. */
    fun import(rawJson: String): ImportResult {
        val payload = try {
            json.decodeFromString<BackupPayload>(rawJson)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid backup file", e)
        }
        return ImportResult(students = payload.students, settings = payload.settings)
    }
}
