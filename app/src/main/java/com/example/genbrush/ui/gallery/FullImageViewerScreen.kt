package com.example.genbrush.ui.gallery

import android.content.ContentValues
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.example.genbrush.data.local.ImageEntry
import com.example.genbrush.data.repository.GenerationRepository
import com.example.genbrush.ui.localization.AppStrings
import com.example.genbrush.ui.localization.LocalStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FullImageViewerScreen(
    imageId: String,
    repository: GenerationRepository,
    onBack: () -> Unit,
    onRegenerate: (String) -> Unit = {},
    onEditImage: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val s = LocalStrings.current

    // Load all images for pager
    val allEntries by produceState<List<ImageEntry>>(initialValue = emptyList()) {
        value = repository.getGalleryImages()
    }

    val initialIndex = remember(allEntries) {
        allEntries.indexOfFirst { it.id == imageId }.coerceAtLeast(0)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }

    // Pager state
    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { allEntries.size }
    )

    val currentEntry = allEntries.getOrNull(pagerState.currentPage)
    val currentFile = currentEntry?.let { repository.getLocalImagePath(it) }

    // Zoom state per page
    var scale by remember(pagerState.currentPage) { mutableFloatStateOf(1f) }
    var offsetX by remember(pagerState.currentPage) { mutableFloatStateOf(0f) }
    var offsetY by remember(pagerState.currentPage) { mutableFloatStateOf(0f) }

    // Delete confirmation dialog
    if (showDeleteDialog && currentEntry != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(s.deleteConfirmTitle) },
            text = { Text(s.deleteConfirmMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    coroutineScope.launch(Dispatchers.IO) {
                        repository.deleteImage(currentEntry)
                        withContext(Dispatchers.Main) { onBack() }
                    }
                }) {
                    Text(s.commonConfirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(s.commonCancel)
                }
            }
        )
    }

    // Info bottom sheet
    if (showInfoSheet && currentEntry != null) {
        ModalBottomSheet(
            onDismissRequest = { showInfoSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    s.viewerInfoTitle,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                InfoRow(s.viewerPrompt, currentEntry.prompt)
                currentEntry.negativePrompt?.takeIf { it.isNotBlank() }?.let {
                    InfoRow(s.viewerNegativePrompt, it)
                }
                InfoRow(s.viewerModel, currentEntry.model)
                currentEntry.size?.let { InfoRow(s.viewerSize, it) }
                InfoRow(s.viewerType, if (currentEntry.type == "text_to_image") s.galleryFilterTxt2img else s.galleryFilterEdit)
                InfoRow(s.viewerTime, SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(currentEntry.timestamp)))
                currentFile?.let { file ->
                    if (file.exists()) {
                        val sizeKb = file.length() / 1024
                        InfoRow(s.viewerFileSize, if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showInfoSheet = false
                            onRegenerate(currentEntry.prompt)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(s.viewerRegenerate, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = {
                            showInfoSheet = false
                            currentFile?.let { onEditImage(it.absolutePath) }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(s.viewerEditImage, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            currentEntry?.prompt ?: s.viewerTitle,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        currentEntry?.let {
                            val sizeLabel = it.size ?: "—"
                            Text(
                                "${it.model} | $sizeLabel | ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it.timestamp))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = s.viewerBack)
                    }
                },
                actions = {
                    if (currentEntry != null) {
                        // Favorite
                        IconButton(onClick = {
                            coroutineScope.launch {
                                repository.setFavorite(currentEntry.id, !currentEntry.isFavorite)
                                // Refresh
                                val refreshed = repository.getGalleryImages()
                                // Trigger recomposition by updating produceState is not possible directly,
                                // but the pager page key change will refresh. For now, reload all entries.
                            }
                        }) {
                            Icon(
                                imageVector = if (currentEntry.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = if (currentEntry.isFavorite) s.galleryUnfavorite else s.galleryFavorite,
                                tint = if (currentEntry.isFavorite) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Info
                        IconButton(onClick = { showInfoSheet = true }) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = s.viewerInfo,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        bottomBar = {
            if (currentEntry != null && currentFile != null && currentFile.exists()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = {
                        coroutineScope.launch(Dispatchers.IO) {
                            val bitmap = BitmapFactory.decodeFile(currentFile.absolutePath)
                            if (bitmap != null) {
                                val fileName = "GenBrush_${currentEntry.timestamp}.jpg"
                                var saved = false
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val contentValues = ContentValues().apply {
                                        put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/GenBrush")
                                    }
                                    val uri = context.contentResolver.insert(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                        contentValues
                                    )
                                    uri?.let {
                                        context.contentResolver.openOutputStream(it)?.use { out ->
                                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                                        }
                                        saved = true
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    MediaStore.Images.Media.insertImage(
                                        context.contentResolver,
                                        bitmap,
                                        fileName,
                                        "由 GenBrush 生成：${currentEntry.prompt}"
                                    )
                                    saved = true
                                }
                                bitmap.recycle()
                                if (saved) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, AppStrings.ZH.commonSaved, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(s.viewerSave)
                    }
                    OutlinedButton(onClick = {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            currentFile
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/jpeg"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, s.viewerShareTitle))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(s.viewerShare)
                    }
                    OutlinedButton(onClick = {
                        showDeleteDialog = true
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(s.viewerDelete)
                    }
                }
            }
        }
    ) { innerPadding ->
        if (allEntries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(s.viewerNotFound, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                beyondViewportPageCount = 1
            ) { page ->
                val entry = allEntries[page]
                val file = repository.getLocalImagePath(entry)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = s.viewerDesc,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            if (scale > 1f) {
                                                scale = 1f; offsetX = 0f; offsetY = 0f
                                            } else {
                                                scale = 2f
                                            }
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                                        if (scale > 1f) {
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                },
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(s.viewerNotFound, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
