package com.example.genbrush.ui.gallery

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil3.compose.AsyncImage
import com.example.genbrush.ui.localization.LocalStrings

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onImageClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val s = LocalStrings.current
    var showSortMenu by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadImages()
    }

    // Delete confirmation dialog
    state.pendingDelete?.let { pendingEntry ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text(s.deleteConfirmTitle) },
            text = { Text(s.deleteConfirmMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text(s.commonConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text(s.commonCancel)
                }
            }
        )
    }

    val displayedImages = viewModel.displayedImages()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(s.galleryTitle) },
            actions = {
                // Sort button
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(s.gallerySortNewest) },
                            onClick = { viewModel.updateSortMode(SortMode.TIME_DESC); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(s.gallerySortOldest) },
                            onClick = { viewModel.updateSortMode(SortMode.TIME_ASC); showSortMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text(s.gallerySortFavFirst) },
                            onClick = { viewModel.updateSortMode(SortMode.FAVORITE_FIRST); showSortMenu = false }
                        )
                    }
                }
                // Favorite filter
                IconButton(onClick = { viewModel.toggleFavoriteFilter() }) {
                    Icon(
                        imageVector = if (state.showFavoritesOnly) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = s.galleryShowFavorites,
                        tint = if (state.showFavoritesOnly) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            placeholder = { Text(s.gallerySearchHint) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Type filter chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = state.typeFilter == null,
                    onClick = { viewModel.updateTypeFilter(null) },
                    label = { Text(s.galleryFilterAll) }
                )
            }
            item {
                FilterChip(
                    selected = state.typeFilter == "text_to_image",
                    onClick = { viewModel.updateTypeFilter("text_to_image") },
                    label = { Text(s.galleryFilterTxt2img) }
                )
            }
            item {
                FilterChip(
                    selected = state.typeFilter == "image_edit",
                    onClick = { viewModel.updateTypeFilter("image_edit") },
                    label = { Text(s.galleryFilterEdit) }
                )
            }
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (displayedImages.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        s.galleryEmpty,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        s.galleryEmptyHint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(displayedImages, key = { it.id }) { entry ->
                    var showMenu by remember { mutableStateOf(false) }
                    val file = remember(entry) { viewModel.getLocalImagePath(entry) }

                    Box {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .combinedClickable(
                                    onClick = { onImageClick(entry.id) },
                                    onLongClick = { showMenu = true }
                                ),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (file.exists()) {
                                    AsyncImage(
                                        model = file,
                                        contentDescription = entry.prompt,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            s.galleryImageNotFound,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                // Favorite badge
                                if (entry.isFavorite) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                shape = CircleShape
                                            )
                                            .padding(2.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = entry.prompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (entry.isFavorite) s.galleryUnfavorite else s.galleryFavorite) },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleFavorite(entry)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(s.gallerySave) },
                                onClick = {
                                    showMenu = false
                                    viewModel.saveToDeviceGallery(entry, context)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(s.galleryShare) },
                                onClick = {
                                    showMenu = false
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/jpeg"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, s.galleryShareTitle))
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(s.galleryDelete) },
                                onClick = {
                                    showMenu = false
                                    viewModel.requestDelete(entry)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(s.galleryCopyPrompt) },
                                onClick = {
                                    showMenu = false
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard.setPrimaryClip(ClipData.newPlainText("prompt", entry.prompt))
                                    Toast.makeText(context, s.galleryPromptCopied, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
