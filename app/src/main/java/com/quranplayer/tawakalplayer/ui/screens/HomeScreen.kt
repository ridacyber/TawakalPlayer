package com.quranplayer.tawakalplayer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranplayer.tawakalplayer.ui.theme.*

@Composable
fun HomeScreen(onEnter: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Maroon, MaroonDark))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                color = Cream,
                fontSize = 30.sp,
                fontFamily = ArefRuqaa,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "TawakalPlayer",
                color = MutedGold,
                fontSize = 20.sp,
                fontFamily = Cormorant,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onEnter,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MutedGold,
                    contentColor = MaroonDark
                ),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(50.dp)
            ) {
                Text(
                    text = "Begin",
                    fontSize = 16.sp,
                    fontFamily = Cormorant,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}