package com.complexly.academy.ui.course

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Video
import com.complexly.academy.ui.home.getChannelColor
import com.complexly.academy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailsScreen(
    viewModel: CourseDetailsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVideo: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.channelTitle.isNotEmpty()) {
                        Text(
                            text = uiState.channelTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground)
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val playlist = uiState.playlist
            if (playlist != null) {
                val channelColor = getChannelColor(playlist.channelId)
                val completedCount = uiState.videos.filter { uiState.completedVideoIds.contains(it.id) }.size
                val totalCount = uiState.videos.size
                val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        CourseHeaderSection(
                            playlist = playlist,
                            isCourse = uiState.isCourse,
                            isEnrolled = uiState.isEnrolled,
                            onEnroll = { viewModel.enroll() },
                            onUnenroll = { viewModel.unenroll() },
                            channelColor = channelColor,
                            completedCount = completedCount,
                            totalCount = totalCount,
                            progress = progress,
                            onResume = {
                                val firstIncomplete = uiState.videos.find { !uiState.completedVideoIds.contains(it.id) }
                                val targetVideoId = firstIncomplete?.id ?: uiState.videos.firstOrNull()?.id
                                targetVideoId?.let { onNavigateToVideo(it, playlist.id) }
                            }
                        )
                    }

                    item {
                        Text(
                            text = "Episodes",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    itemsIndexed(uiState.videos) { index, video ->
                        val isCompleted = uiState.completedVideoIds.contains(video.id)
                        val isWatched = uiState.watchedVideoIds.contains(video.id)
                        val isLocked = uiState.isCourse && !uiState.isEnrolled
                        EpisodeRow(
                            index = index + 1,
                            video = video,
                            isCompleted = isCompleted,
                            isWatched = isWatched,
                            isLocked = isLocked,
                            channelColor = channelColor,
                            onClick = {
                                if (!isLocked) {
                                    onNavigateToVideo(video.id, playlist.id)
                                }
                            }
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Playlist not found", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun CourseHeaderSection(
    playlist: Playlist,
    isCourse: Boolean,
    isEnrolled: Boolean,
    onEnroll: () -> Unit,
    onUnenroll: () -> Unit,
    channelColor: Color,
    completedCount: Int,
    totalCount: Int,
    progress: Float,
    onResume: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(channelColor.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = playlist.channelId.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = channelColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = playlist.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = playlist.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MediumGrayText
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Progress stats
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$completedCount of $totalCount Completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGrayText
                )
                Text(
                    text = "${(progress * 100).toInt()}% Complete",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { progress },
                color = channelColor,
                trackColor = CardBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (isCourse && !isEnrolled) {
                Button(
                    onClick = onEnroll,
                    colors = ButtonDefaults.buttonColors(containerColor = channelColor),
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enroll in Course",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onResume,
                        colors = ButtonDefaults.buttonColors(containerColor = channelColor),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        val label = when {
                            progress > 0 && isCourse -> "Resume Course"
                            progress > 0 -> "Resume Playlist"
                            isCourse -> "Start Course"
                            else -> "Start Playlist"
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (isCourse && isEnrolled) {
                        OutlinedButton(
                            onClick = onUnenroll,
                            border = BorderStroke(1.dp, channelColor),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = channelColor),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = "Unenroll",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeRow(
    index: Int,
    video: Video,
    isCompleted: Boolean,
    isWatched: Boolean,
    isLocked: Boolean,
    channelColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color.White.copy(alpha = 0.6f) else Color.White
        ),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = !isLocked) { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                // Dark overlay for play icon contrast
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )
                // Episode number badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = index.toString().padStart(2, '0'),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isLocked) CharcoalText.copy(alpha = 0.6f) else CharcoalText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${video.durationSeconds / 60} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGrayText
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isLocked) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = MediumGrayText,
                    modifier = Modifier.size(24.dp)
                )
            } else if (isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = "Learned",
                    tint = CorrectGreen,
                    modifier = Modifier.size(24.dp)
                )
            } else if (isWatched) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = "Watched",
                    tint = channelColor,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(channelColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play Episode",
                        tint = channelColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
