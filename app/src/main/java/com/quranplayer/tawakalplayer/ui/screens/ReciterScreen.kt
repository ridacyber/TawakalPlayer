package com.quranplayer.tawakalplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplayer.tawakalplayer.data.Reciter
import com.quranplayer.tawakalplayer.data.ReciterRepository
import com.quranplayer.tawakalplayer.ui.theme.*

@Composable
fun ReciterScreen(onReciterSelected: (Reciter) -> Unit, onBack: () -> Unit) {
    var reciters by remember { mutableStateOf<List<Reciter>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            reciters = ReciterRepository.fetchTargetReciters()
        } catch (e: Exception) {
            error = e.message ?: "Failed to load reciters"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaroonDark)
    ) {
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MutedGold)
                }
            }
            error != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Couldn't load reciters:\n$error",
                        color = Cream,
                        fontFamily = Cormorant,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Choose a Reciter",
                        color = MutedGold,
                        fontSize = 22.sp,
                        fontFamily = Cormorant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 76.dp, top = 28.dp, end = 24.dp, bottom = 4.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(reciters) { reciter ->
                            ReciterCard(reciter = reciter, onClick = { onReciterSelected(reciter) })
                        }
                    }
                }
            }
        }

        BackButton(onBack = onBack, modifier = Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun ReciterCard(reciter: Reciter, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Maroon)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = reciter.displayName,
            color = Cream,
            fontSize = 18.sp,
            fontFamily = Cormorant,
            fontWeight = FontWeight.Medium
        )
    }
}