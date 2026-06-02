package com.complexly.academy.ui.progress

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
fun ProgressScreen(
    viewModel: ProgressViewModel,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToCourse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val progress = uiState.progress

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Your Progress",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Videos Watched",
                            value = "${progress.completedVideoIds.size}",
                            icon = Icons.Rounded.PlayCircle,
                            color = CrashCourseTeal,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Correct Answers",
                            value = "${progress.correctAnswersCount}",
                            icon = Icons.Rounded.CheckCircle,
                            color = SciShowBlue,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Current Streak",
                            value = "${progress.streakCount} days",
                            icon = Icons.Rounded.LocalFireDepartment,
                            color = EonsAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Courses Section
                if (uiState.inProgressCourses.isNotEmpty() || uiState.completedCourses.isNotEmpty()) {
                    item {
                        Text(
                            text = "My Courses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(uiState.inProgressCourses) { (playlist, percent) ->
                        CourseProgressCard(
                            playlist = playlist,
                            progress = percent,
                            isCompleted = false,
                            channelColor = getChannelColor(playlist.channelId),
                            onClick = { onNavigateToCourse(playlist.id) }
                        )
                    }

                    items(uiState.completedCourses) { playlist ->
                        CourseProgressCard(
                            playlist = playlist,
                            progress = 1.0f,
                            isCompleted = true,
                            channelColor = getChannelColor(playlist.channelId),
                            onClick = { onNavigateToCourse(playlist.id) }
                        )
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MenuBook,
                                    contentDescription = null,
                                    tint = MediumGrayText,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Not Enrolled in Any Courses Yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CharcoalText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "To start a course, enroll explicitly inside any course details screen.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MediumGrayText,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }

                // Recent Videos Section
                if (uiState.recentVideos.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Videos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(uiState.recentVideos) { video ->
                                val channelId = video.playlistIds.firstOrNull()?.let { plId ->
                                    uiState.inProgressCourses.find { it.first.id == plId }?.first?.channelId
                                        ?: uiState.completedCourses.find { it.id == plId }?.channelId
                                } ?: "scishow"
                                RecentVideoCard(
                                    video = video,
                                    channelId = channelId,
                                    channelColor = getChannelColor(channelId),
                                    isCompleted = progress.completedVideoIds.contains(video.id),
                                    onClick = { onNavigateToVideo(video.id) }
                                )
                            }
                        }
                    }
                }

                // Badges Section
                item {
                    Text(
                        text = "Unlocked Achievements",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    BadgeRow(
                        badgeName = "Early Adopter",
                        badgeDesc = "Joined Academy KMP prototype",
                        icon = Icons.Rounded.Stars,
                        color = SelectedNavIcon,
                        isUnlocked = true
                    )
                }

                item {
                    BadgeRow(
                        badgeName = "Curious Mind",
                        badgeDesc = "Answered the daily question of the day",
                        icon = Icons.Rounded.Lightbulb,
                        color = EonsAmber,
                        isUnlocked = progress.streakCount > 0
                    )
                }

                item {
                    BadgeRow(
                        badgeName = "Learning Master",
                        badgeDesc = "Watch 10 educational videos",
                        icon = Icons.Rounded.School,
                        color = SciShowBlue,
                        isUnlocked = progress.completedVideoIds.size >= 10
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MediumGrayText)
        }
    }
}

@Composable
fun BadgeRow(
    badgeName: String,
    badgeDesc: String,
    icon: ImageVector,
    color: Color,
    isUnlocked: Boolean
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnlocked) Color.White else Color.White.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isUnlocked) color.copy(alpha = 0.1f) else CardBorder),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isUnlocked) color else MediumGrayText,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = badgeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) CharcoalText else MediumGrayText
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = badgeDesc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediumGrayText
                )
            }
        }
    }
}

@Composable
fun CourseProgressCard(
    playlist: Playlist,
    progress: Float,
    isCompleted: Boolean,
    channelColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = playlist.channelId.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = channelColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LinearProgressIndicator(
                        progress = { progress },
                        color = channelColor,
                        trackColor = CardBorder,
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isCompleted) "Completed" else "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) CorrectGreen else CharcoalText
                    )
                }
            }
        }
    }
}

@Composable
fun RecentVideoCard(
    video: Video,
    channelId: String,
    channelColor: Color,
    isCompleted: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                if (isCompleted) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.School,
                            contentDescription = null,
                            tint = CorrectGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(40.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = channelId.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = channelColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${video.durationSeconds / 60}m",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGrayText
                )
            }
        }
    }
}
