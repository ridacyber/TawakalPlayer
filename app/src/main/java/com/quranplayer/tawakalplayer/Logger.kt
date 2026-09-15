package com.quranplayer.tawakalplayer

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Writes log lines to a plain text file so we can inspect overnight playback
 * behavior without needing a laptop connected live via adb.
 *
 * File lives at: Android/data/com.quranplayer.tawakalplayer/files/playback_log.txt
 * which is visible over USB (MTP) or through any file manager app — no adb needed.
 *
 * Caps the file at ~1MB and trims the oldest half when exceeded, so it never
 * grows unbounded on a device that's rarely connected to a computer.
 */
object Logger {
    private const val FILE_NAME = "playback_log.txt"
    private const val MAX_BYTES = 1_000_000L
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @Synchronized
    fun log(context: Context, message: String) {
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, FILE_NAME)

            if (file.exists() && file.length() > MAX_BYTES) {
                val lines = file.readLines()
                val trimmed = lines.drop(lines.size / 2)
                file.writeText(trimmed.joinToString("\n") + "\n")
            }

            val timestamp = timestampFormat.format(Date())
            file.appendText("[$timestamp] $message\n")
        } catch (e: Exception) {
            // Logging must never crash playback — swallow any file I/O error.
        }
    }
}