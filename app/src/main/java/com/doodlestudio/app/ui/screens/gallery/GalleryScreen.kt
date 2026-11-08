package com.doodlestudio.app.ui.screens.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.doodlestudio.app.R
import com.doodlestudio.app.domain.model.Artwork
import com.doodlestudio.app.ui.components.PromoAppsList
import com.doodlestudio.app.ui.components.PromoGalleryCard
import com.doodlestudio.app.ui.components.openPlayStore
import com.doodlestudio.app.ui.theme.NeonBackgroundGallery
import com.doodlestudio.app.ui.theme.NeonCoralButton
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.util.SoundEffectManager
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.io.File

@Composable
fun GalleryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit,
    viewModel: GalleryViewModel = koinViewModel(),
    soundEffectManager: SoundEffectManager = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedArtworkToDelete by remember { mutableStateOf<Artwork?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonBackgroundGallery)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar: Coral circular Back Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NeonCoralButton)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                soundEffectManager.play(SoundEffectManager.Sound.CLICK)
                                onNavigateBack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "❮", color = Color.White, fontSize = 20.sp)
                }
            }

            // 3-Column Grid of Artworks & Promo Tiles
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // 1. User's Saved Artworks (first items)
                items(uiState.artworks, key = { it.id }) { artwork ->
                    ArtworkGridCard(
                        artwork = artwork,
                        onClick = {
                            soundEffectManager.play(SoundEffectManager.Sound.CLICK)
                            onNavigateToEditor(artwork.id)
                        },
                        onLongClick = {
                            selectedArtworkToDelete = artwork
                        }
                    )
                }

                // Empty state hint if user has no saved artworks
                if (uiState.artworks.isEmpty()) {
                    item(span = { GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.gallery_empty_neon),
                                color = Color.Gray,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 2. Following items = Promo Apps Tiles (config list)
                items(PromoAppsList, key = { it.id }) { promoApp ->
                    PromoGalleryCard(
                        app = promoApp,
                        onClick = {
                            openPlayStore(context, promoApp.packageName)
                        }
                    )
                }
            }

            // Bottom Footer: Centered "N Artworks" live count
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.artworks_count, uiState.artworkCount),
                    color = Color(0xFFCCCCCC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Delete Artwork Confirmation Dialog
        selectedArtworkToDelete?.let { art ->
            AlertDialog(
                onDismissRequest = { selectedArtworkToDelete = null },
                title = { Text(text = "Xóa tác phẩm") },
                text = { Text(text = "Bạn có chắc chắn muốn xóa tác phẩm này khỏi bộ sưu tập không?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteArtwork(art.id)
                            selectedArtworkToDelete = null
                        }
                    ) {
                        Text(text = "Xóa", color = NeonCoralButton)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedArtworkToDelete = null }) {
                        Text(text = "Hủy", color = Color.Gray)
                    }
                },
                containerColor = Color.White,
                titleContentColor = Color.Black,
                textContentColor = Color.DarkGray
            )
        }
    }
}

/**
 * 3-Column Saved Artwork Card:
 * Aspect ratio ~ 3:4, white card, subtle shadow, thin inner gray border, black thumbnail.
 */
@Composable
private fun ArtworkGridCard(
    artwork: Artwork,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val thumbnailBitmap = remember(artwork.thumbnailPath) {
        artwork.thumbnailPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap.asImageBitmap(),
                        contentDescription = artwork.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = "Neon",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
