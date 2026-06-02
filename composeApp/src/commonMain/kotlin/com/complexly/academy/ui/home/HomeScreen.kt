package com.complexly.academy.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.complexly.academy.model.Channel
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Video
import com.complexly.academy.ui.discover.formatViewCount
import com.complexly.academy.ui.theme.*
import com.complexly.academy.domain.RecentViewItem

@Composable
fun ComplexlyLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF4F378B)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
            val w = size.width
            val h = size.height
            val center = Offset(w / 2f, h / 2f)

            // Surrounding node positions (normalized offsets from center)
            val outerOffsets = listOf(
                Offset(-0.35f, -0.3f), // Top-left
                Offset(0.35f, -0.35f),  // Top-right
                Offset(-0.3f, 0.35f),  // Bottom-left
                Offset(0.35f, 0.3f),   // Bottom-right
                Offset(-0.4f, 0.05f),   // Left
                Offset(0.4f, -0.05f)    // Right
            )

            // Connections: center to all outer nodes, plus some outer interconnections
            val connections = listOf(
                0 to 1,
                0 to 2,
                0 to 3,
                0 to 4,
                0 to 5,
                0 to 6,
                1 to 5,
                2 to 6,
                3 to 5,
                4 to 6
            )

            val nodeCoords = listOf(center) + outerOffsets.map {
                Offset(center.x + it.x * w, center.y + it.y * h)
            }

            val strokeWidth = 1.5.dp.toPx()
            val centralRadius = 3.5.dp.toPx()
            val outerRadius = 2.5.dp.toPx()

            // Draw connection lines
            connections.forEach { (from, to) ->
                if (from < nodeCoords.size && to < nodeCoords.size) {
                    drawLine(
                        color = Color.White,
                        start = nodeCoords[from],
                        end = nodeCoords[to],
                        strokeWidth = strokeWidth
                    )
                }
            }

            // Draw nodes
            nodeCoords.forEachIndexed { index, coord ->
                drawCircle(
                    color = Color.White,
                    radius = if (index == 0) centralRadius else outerRadius,
                    center = coord
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToCourse: (String) -> Unit,
    onNavigateToQuiz: (String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onChannelClick: (String) -> Unit,  // passes channel ID
    onStartReview: () -> Unit = {},
    dueReviewCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Stay curious.",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText,
                        fontSize = 28.sp
                    )
                },
                actions = {
                    // Streak Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White)
                            .border(BorderStroke(1.dp, CardBorder), RoundedCornerShape(20.dp))
                            .clickable { viewModel.onStreakClicked() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = EonsAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${uiState.streakCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CharcoalText
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
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
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Home Pager (Review, Daily Question, Store Hero)
                item {
                    HomePager(
                        dueReviewCount = dueReviewCount,
                        dailyQuestion = uiState.dailyQuestion,
                        onStartReview = onStartReview,
                        onStartQuiz = { onNavigateToQuiz("daily") },
                        onOpenStoreLink = {} // TODO: wire to store navigation
                    )
                }

                // Pick up where you left off
                item {
                    SectionHeader(
                        title = "Pick up where you left off",
                        icon = Icons.Rounded.PlayCircleOutline
                    )

                    val recentlyViewed = uiState.recentlyViewedItems
                    if (recentlyViewed.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(recentlyViewed) { item ->
                                when (item) {
                                    is RecentViewItem.VideoItem -> {
                                        val video = item.video
                                        val firstPlId = video.playlistIds.firstOrNull()
                                        val playlist = uiState.playlists.find { it.id == firstPlId }
                                        val channel = playlist?.let { p -> uiState.channels.find { it.id == p.channelId } }
                                        VideoCard(
                                            video = video,
                                            channelName = channel?.title ?: "Complexly",
                                            channelColor = getChannelColor(playlist?.channelId ?: ""),
                                            onClick = { onNavigateToVideo(video.id) }
                                        )
                                    }
                                    is RecentViewItem.PlaylistItem -> {
                                        val playlist = item.playlist
                                        val channel = uiState.channels.find { it.id == playlist.channelId }
                                        val playlistVideos = uiState.videos.filter { playlist.id in it.playlistIds }
                                        ContinueCourseCard(
                                            playlist = playlist,
                                            isCourse = item.isCourse,
                                            channelName = channel?.title ?: "Science",
                                            channelColor = getChannelColor(playlist.channelId),
                                            completedCount = playlistVideos.count { it.id in uiState.completedVideoIds },
                                            onClick = { onNavigateToCourse(playlist.id) }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        EmptyStateText(text = "No active items. Explore below!")
                    }
                }

                // Browse Channels
                item {
                    SectionHeader(
                        title = "Browse channels",
                        icon = Icons.Rounded.GridView
                    )

                    if (uiState.channels.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(uiState.channels) { channel ->
                                ChannelCard(
                                    channel = channel,
                                    onClick = { onChannelClick(channel.id) }
                                )
                            }
                        }
                    } else {
                        EmptyStateText(text = "No channels available.")
                    }
                }

                // Most popular playlists
                item {
                    SectionHeader(
                        title = "Most popular playlists",
                        icon = Icons.AutoMirrored.Rounded.TrendingUp
                    )

                    val popularPlaylists = uiState.popularPlaylists
                    if (popularPlaylists.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(popularPlaylists) { playlist ->
                                val channel = uiState.channels.find { it.id == playlist.channelId }
                                PlaylistCarouselCard(
                                    playlist = playlist,
                                    isCourse = playlist.isCourse,
                                    channelName = channel?.title ?: "Science",
                                    channelColor = getChannelColor(playlist.channelId),
                                    firstVideoThumbnail = playlist.thumbnailUrl.ifEmpty { null },
                                    extraInfo = "${formatViewCount(playlist.avgViewCount)} avg",
                                    onClick = { onNavigateToCourse(playlist.id) }
                                )
                            }
                        }
                    } else {
                        EmptyStateText(text = "No popular playlists available.")
                    }
                }

                // Highest rated playlists
                item {
                    SectionHeader(
                        title = "Highest rated playlists",
                        icon = Icons.Rounded.Star
                    )

                    val ratedPlaylists = uiState.highestRatedPlaylists
                    if (ratedPlaylists.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(ratedPlaylists) { playlist ->
                                val channel = uiState.channels.find { it.id == playlist.channelId }
                                PlaylistCarouselCard(
                                    playlist = playlist,
                                    isCourse = playlist.isCourse,
                                    channelName = channel?.title ?: "Science",
                                    channelColor = getChannelColor(playlist.channelId),
                                    firstVideoThumbnail = playlist.thumbnailUrl.ifEmpty { null },
                                    extraInfo = "★ " + formatRating(playlist.avgRating),
                                    onClick = { onNavigateToCourse(playlist.id) }
                                )
                            }
                        }
                    } else {
                        EmptyStateText(text = "No highly rated playlists available.")
                    }
                }

                // Popular videos
                item {
                    SectionHeader(
                        title = "Most popular",
                        icon = Icons.Rounded.LocalFireDepartment
                    )

                    val popular = uiState.videos
                        .filter { it.viewCount > 0 }
                        .sortedByDescending { it.viewCount }
                        .take(5)
                    if (popular.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(popular) { video ->
                                val firstPlId = video.playlistIds.firstOrNull()
                                val playlist = uiState.playlists.find { it.id == firstPlId }
                                val channel = playlist?.let { p -> uiState.channels.find { it.id == p.channelId } }
                                VideoCard(
                                    video = video,
                                    channelName = channel?.title ?: firstPlId.orEmpty(),
                                    channelColor = getChannelColor(channel?.id ?: ""),
                                    onClick = { onNavigateToVideo(video.id) }
                                )
                            }
                        }
                    } else {
                        EmptyStateText(text = "No popular videos yet.")
                    }
                }

                // New this week (Videos horizontal list)
                item {
                    SectionHeader(
                        title = "New this week",
                        icon = Icons.Rounded.NewReleases,
                        showAllAction = true
                    )

                    val latestVideos = uiState.videos.take(5)
                    if (latestVideos.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(latestVideos) { video ->
                                val firstPlId = video.playlistIds.firstOrNull()
                                val playlist = uiState.playlists.find { it.id == firstPlId }
                                val channel = playlist?.let { p -> uiState.channels.find { it.id == p.channelId } }
                                VideoCard(
                                    video = video,
                                    channelName = channel?.title ?: "Complexly",
                                    channelColor = getChannelColor(playlist?.channelId ?: ""),
                                    onClick = { onNavigateToVideo(video.id) }
                                )
                            }
                        }
                    } else {
                        EmptyStateText(text = "No new videos this week.")
                    }
                }

                // Recommended for You
                item {
                    SectionHeader(
                        title = "Recommended for you",
                        icon = Icons.Rounded.AutoAwesome
                    )

                    val recommendedVideos = uiState.recommendedVideos
                    if (recommendedVideos.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            items(recommendedVideos) { video ->
                                val firstPlId = video.playlistIds.firstOrNull()
                                val playlist = uiState.playlists.find { it.id == firstPlId }
                                val channel = playlist?.let { p -> uiState.channels.find { it.id == p.channelId } }
                                VideoCard(
                                    video = video,
                                    channelName = channel?.title ?: "Complexly",
                                    channelColor = getChannelColor(playlist?.channelId ?: ""),
                                    onClick = { onNavigateToVideo(video.id) }
                                )
                            }
                        }
                    } else {
                        EmptyStateText(text = "Explore more courses to get recommendations!")
                    }
                }
            }
        }
    }
}

@Composable
fun DailyQuestionSection(
    questionText: String,
    onStartQuiz: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = EonsAmber),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = "Question of the Day Icon",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "QUESTION OF THE DAY",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = questionText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onStartQuiz,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = EonsAmber
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Reveal the answer",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = EonsAmber
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        tint = EonsAmber,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueCourseCard(
    playlist: Playlist,
    isCourse: Boolean,
    channelName: String,
    channelColor: Color,
    completedCount: Int,
    onClick: () -> Unit
) {
    val totalCount = playlist.videoIds.size
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val typeLabel = if (isCourse) "Course" else "Playlist"

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .width(200.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                // Dark overlay for contrast
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )

                // Type badge (Course or Playlist)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isCourse) SelectedNavIcon.copy(alpha = 0.85f) else channelColor.copy(alpha = 0.85f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = typeLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }

                // Progress percentage overlay in bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }

                // Progress Bar at the very bottom of the thumbnail
                LinearProgressIndicator(
                    progress = { progress },
                    color = channelColor,
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Channel label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(channelColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGrayText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Completion status
                Text(
                    text = "$completedCount of $totalCount done",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGrayText
                )
            }
        }
    }
}

@Composable
fun VideoCard(
    video: Video,
    channelName: String,
    channelColor: Color,
    onClick: () -> Unit
) {
    val minutes = video.durationSeconds / 60
    val seconds = video.durationSeconds % 60
    val durationText = "${minutes}:${seconds.toString().padStart(2, '0')}"

    Column(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        // Real YouTube thumbnail via Coil
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // Dark overlay for contrast
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )

            // Play Button
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play Video",
                    tint = CharcoalText,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Duration Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Channel Dot & Name
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(channelColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = channelName,
                style = MaterialTheme.typography.labelSmall,
                color = MediumGrayText
            )
            // View count pill
            if (video.viewCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MediumGrayText.copy(alpha = 0.12f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = formatViewCount(video.viewCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGrayText
                    )
                }
            }
        }

        // Title
        Text(
            text = video.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CharcoalText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit
) {
    val channelColor = getChannelColor(channel.id)
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .width(120.dp)
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Channel logo image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(channelColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (channel.image.isNotEmpty()) {
                    AsyncImage(
                        model = channel.image,
                        contentDescription = channel.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback letter if image is empty
                    Text(
                        text = channel.title.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = channelColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = channel.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = CharcoalText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    showAllAction: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CharcoalText,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = CharcoalText
            )
        }
        if (showAllAction) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "All",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediumGrayText
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MediumGrayText,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyStateText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MediumGrayText,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

fun getChannelColor(channelId: String): Color {
    val hex = com.complexly.academy.model.ChannelColorRegistry.getColor(channelId)
    if (hex != null) {
        try {
            return Color(hex.removePrefix("#").toLong(16) or 0xFF000000)
        } catch (e: Exception) {
            // fallback
        }
    }
    return when (channelId) {
        "crashcourse" -> CrashCourseTeal
        "scishow" -> SciShowBlue
        "pbseons", "eons" -> EonsAmber
        else -> BizarreBeastsPurple
    }
}

@Composable
fun PlaylistCarouselCard(
    playlist: Playlist,
    isCourse: Boolean,
    channelName: String,
    channelColor: Color,
    firstVideoThumbnail: String?,
    extraInfo: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.width(220.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Thumbnail Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(channelColor.copy(alpha = 0.08f))
            ) {
                if (firstVideoThumbnail != null) {
                    AsyncImage(
                        model = firstVideoThumbnail,
                        contentDescription = playlist.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Color.Black.copy(alpha = 0.15f))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                val step = 12.dp.toPx()
                                val sw = 2.dp.toPx()
                                val stripeColor = channelColor.copy(alpha = 0.15f)
                                var x = 0f
                                while (x < size.width + size.height) {
                                    drawLine(stripeColor, Offset(x, 0f), Offset(x - size.height, size.height), sw)
                                    x += step
                                }
                            }
                    )
                }

                // Course / Playlist badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCourse) channelColor else Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isCourse) "COURSE" else "PLAYLIST",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Info Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Channel name with dot
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(channelColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGrayText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Playlist Title
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CharcoalText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    minLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats / Extra info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${playlist.videoIds.size} videos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGrayText
                    )
                    
                    Text(
                        text = extraInfo,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = channelColor
                    )
                }
            }
        }
    }
}

fun formatRating(rating: Double): String {
    val rounded = (rating * 100).toInt() / 100.0
    val parts = rounded.toString().split(".")
    if (parts.size == 1) return "${parts[0]}.00"
    val decimal = parts[1]
    return when (decimal.length) {
        0 -> "${parts[0]}.00"
        1 -> "${parts[0]}.${decimal}0"
        else -> "${parts[0]}.${decimal.take(2)}"
    }
}
