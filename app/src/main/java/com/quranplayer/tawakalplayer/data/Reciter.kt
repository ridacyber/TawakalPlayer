package com.quranplayer.tawakalplayer.data

data class Reciter(
    val id: Int,
    val displayName: String,
    val serverUrl: String // base URL, e.g. https://server8.mp3quran.net/basit/
)