package com.quranplayer.tawakalplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.quranplayer.tawakalplayer.ui.theme.MaroonDark
import com.quranplayer.tawakalplayer.ui.theme.MutedGold

@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(20.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(MutedGold)
            .clickable { onBack() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaroonDark
        )
    }
}