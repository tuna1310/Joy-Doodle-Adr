package com.doodlestudio.app.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.doodlestudio.app.R
import com.doodlestudio.app.data.preferences.SettingsDataStore
import com.doodlestudio.app.ui.theme.CoralPink
import com.doodlestudio.app.ui.theme.MintGreen
import com.doodlestudio.app.ui.theme.OceanBlue
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class OnboardingPageData(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    settingsDataStore: SettingsDataStore = koinInject()
) {
    val pages = listOf(
        OnboardingPageData(
            titleRes = R.string.onboarding_title_1,
            descRes = R.string.onboarding_desc_1,
            icon = Icons.Default.Star,
            color = CoralPink
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_title_2,
            descRes = R.string.onboarding_desc_2,
            icon = Icons.Default.Create,
            color = OceanBlue
        ),
        OnboardingPageData(
            titleRes = R.string.onboarding_title_3,
            descRes = R.string.onboarding_desc_3,
            icon = Icons.Default.PlayArrow,
            color = MintGreen
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    fun completeOnboarding() {
        scope.launch {
            settingsDataStore.setOnboardingCompleted(true)
            onFinish()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Top Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < pages.size - 1) {
                TextButton(onClick = { completeOnboarding() }) {
                    Text(
                        text = stringResource(id = R.string.btn_skip),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }

        // Pager Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(page.color.copy(alpha = 0.15f), shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = null,
                        tint = page.color,
                        modifier = Modifier.size(72.dp)
                    )
                }
                Spacer(modifier = Modifier.height(40.dp))
                Text(
                    text = stringResource(id = page.titleRes),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(id = page.descRes),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dots indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isSelected) CoralPink else Color.LightGray.copy(alpha = 0.5f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom CTA Button
        Button(
            onClick = {
                if (pagerState.currentPage < pages.size - 1) {
                    scope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                } else {
                    completeOnboarding()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CoralPink)
        ) {
            Text(
                text = stringResource(
                    id = if (pagerState.currentPage < pages.size - 1) R.string.btn_next else R.string.btn_start
                ),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

