package com.quranplayer.tawakalplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplayer.tawakalplayer.data.QuranData
import com.quranplayer.tawakalplayer.data.Surah
import com.quranplayer.tawakalplayer.ui.theme.*
import kotlin.math.min

@Composable
fun SurahScreen(onSurahSelected: (Surah) -> Unit, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }

    val filteredSurahs by remember {
        derivedStateOf {
            if (query.isBlank()) {
                QuranData.surahs
            } else {
                QuranData.surahs
                    .map { surah -> surah to surahMatchScore(surah, query) }
                    .filter { (_, score) -> score < Int.MAX_VALUE }
                    .sortedBy { (_, score) -> score }
                    .map { (surah, _) -> surah }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaroonDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 76.dp, top = 28.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Select a Surah",
                    color = MutedGold,
                    fontSize = 22.sp,
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold
                )

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            text = "Search...",
                            color = GoldSoft,
                            fontSize = 13.sp,
                            fontFamily = Cormorant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = GoldSoft,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { query = "" },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "Clear search",
                                    tint = GoldSoft,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Cream,
                        fontSize = 14.sp,
                        fontFamily = Cormorant
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MutedGold,
                        unfocusedBorderColor = GoldSoft.copy(alpha = 0.4f),
                        cursorColor = MutedGold
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(150.dp)
                        .height(46.dp)
                )
            }

            if (filteredSurahs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No surah found",
                        color = GoldSoft,
                        fontSize = 15.sp,
                        fontFamily = Cormorant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSurahs, key = { it.number }) { surah ->
                        SurahCard(surah = surah, onClick = { onSurahSelected(surah) })
                    }
                }
            }
        }

        BackButton(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun SurahCard(surah: Surah, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Maroon)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "${surah.number}. ${surah.nameTransliteration}",
                color = Cream,
                fontSize = 17.sp,
                fontFamily = Cormorant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = surah.nameMeaning,
                color = GoldSoft,
                fontSize = 13.sp,
                fontFamily = Cormorant
            )
        }
        Text(
            text = surah.nameArabic,
            color = MutedGold,
            fontSize = 20.sp,
            fontFamily = ArefRuqaa
        )
    }
}

/**
 * Returns a "distance" score for how well [surah] matches [rawQuery] — lower is better.
 * Int.MAX_VALUE means "no match, filter it out."
 *
 * Friendly/forgiving on purpose:
 * - Case-insensitive
 * - Ignores spaces, hyphens, and apostrophes (so "al-baqarah", "Al Baqarah", "albaqarah" all match)
 * - Exact substring match wins immediately (score 0)
 * - Otherwise falls back to typo-tolerant fuzzy matching (edit distance) against each word,
 *   so small misspellings like "baqara" or "baqrah" still find "Baqarah"
 * - Also matches directly on surah number (typing "2" finds Al-Baqarah)
 */
private fun surahMatchScore(surah: Surah, rawQuery: String): Int {
    val query = normalize(rawQuery)
    if (query.isEmpty()) return 0

    // Direct number match, e.g. typing "2" or "36"
    if (surah.number.toString() == rawQuery.trim()) return 0

    val candidates = listOf(
        normalize(surah.nameTransliteration),
        normalize(surah.nameMeaning)
    )

    var best = Int.MAX_VALUE

    for (candidate in candidates) {
        if (candidate.isEmpty()) continue

        // Exact substring match anywhere — best possible score
        if (candidate.contains(query)) {
            return 0
        }

        // Typo-tolerant: compare query against the whole candidate and against
        // each individual word in it, using edit distance. Threshold scales
        // with query length so short queries aren't overly forgiving.
        val maxAllowedDistance = when {
            query.length <= 3 -> 1
            query.length <= 6 -> 2
            else -> 3
        }

        val words = candidate.split(" ").filter { it.isNotEmpty() } + candidate
        for (word in words) {
            val distance = levenshtein(query, word)
            if (distance <= maxAllowedDistance && distance < best) {
                best = distance + 1 // keep exact substring (0) ranked above any fuzzy match
            }
        }
    }

    return best
}

private fun normalize(text: String): String {
    return text.lowercase().filter { it.isLetterOrDigit() }
}

private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length

    val prev = IntArray(b.length + 1) { it }
    val curr = IntArray(b.length + 1)

    for (i in 1..a.length) {
        curr[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            curr[j] = min(
                min(curr[j - 1] + 1, prev[j] + 1),
                prev[j - 1] + cost
            )
        }
        for (j in 0..b.length) {
            prev[j] = curr[j]
        }
    }
    return prev[b.length]
}