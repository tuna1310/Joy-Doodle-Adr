package com.doodlestudio.app.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.doodlestudio.app.domain.model.Template
import com.doodlestudio.app.domain.model.TemplateCategory
import com.doodlestudio.app.ui.components.AssetTemplateImage
import com.doodlestudio.app.ui.theme.NeonCyan
import com.doodlestudio.app.ui.theme.NeonPink
import com.doodlestudio.app.ui.theme.NeonYellow

@Composable
fun TemplatePickerDialog(
    templates: List<Template>,
    currentTemplateId: String?,
    onSelectTemplate: (Template) -> Unit,
    onSelectBlankCanvas: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(TemplateCategory.ALL) }

    val filteredTemplates = remember(templates, selectedCategory) {
        if (selectedCategory == TemplateCategory.ALL) {
            templates
        } else {
            templates.filter {
                it.categoryId.equals(selectedCategory.id, ignoreCase = true) ||
                it.category.equals(selectedCategory.displayNameVi, ignoreCase = true) ||
                it.category.equals(selectedCategory.displayNameEn, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0F172A)) // Deep slate navy
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(NeonCyan, NeonPink)),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 18.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "🎨", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tranh tô màu",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Chọn hình mẫu để tô màu neon rực rỡ",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Category Chips Row
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(TemplateCategory.entries.toTypedArray()) { cat ->
                        val isSelected = cat == selectedCategory
                        val catIcon = when (cat) {
                            TemplateCategory.ALL -> "⭐"
                            TemplateCategory.ANIMALS -> "🐶"
                            TemplateCategory.VEHICLES -> "🚗"
                            TemplateCategory.SEA -> "🌊"
                            TemplateCategory.FLOWERS -> "🌸"
                            TemplateCategory.DINOSAURS -> "🦖"
                            TemplateCategory.FOOD -> "🍎"
                            TemplateCategory.SCENERY -> "🏡"
                            TemplateCategory.LETTERS -> "🔤"
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    if (isSelected) Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF06B6D4)))
                                    else Brush.horizontalGradient(listOf(Color(0xFF1E293B), Color(0xFF1E293B)))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) NeonCyan else Color(0xFF334155),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$catIcon ${cat.displayNameVi}",
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Templates Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    // Blank Canvas Option Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.86f)
                                .clickable {
                                    onSelectBlankCanvas()
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (currentTemplateId == null) NeonPink else Color(0xFF334155)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "✏️", fontSize = 28.sp)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Vẽ tự do",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Trang vẽ trắng",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    // 40 Template Items
                    items(filteredTemplates) { template ->
                        val isCurrent = template.id == currentTemplateId

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.86f)
                                .clickable {
                                    onSelectTemplate(template)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(
                                if (isCurrent) 2.dp else 1.dp,
                                if (isCurrent) NeonCyan else Color(0xFF334155)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                // Image Thumbnail with dark canvas
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AssetTemplateImage(
                                        assetPath = template.imagePath,
                                        contentDescription = template.displayTitleVi,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Category badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xCC0F172A))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = template.category,
                                            color = NeonCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Title in Vietnamese
                                Text(
                                    text = template.displayTitleVi,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                // Region count hint
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = template.displayTitleEn,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (template.significantRegions > 0) {
                                        Text(
                                            text = "${template.significantRegions} vùng",
                                            color = NeonYellow,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

