package com.complexly.academy.ui.video

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.complexly.academy.model.Video
import com.complexly.academy.platform.openYouTubeVideo
import com.complexly.academy.platform.openExternalLink
import com.complexly.academy.ui.home.getChannelColor
import com.complexly.academy.ui.home.VideoCard
import com.complexly.academy.ui.home.PlaylistCarouselCard
import com.complexly.academy.ui.theme.*
import com.complexly.academy.ui.discover.formatViewCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: VideoPlayerViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToQuiz: (String) -> Unit,
    onNavigateToCourse: (String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.channelName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CharcoalText
                    )
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
            val video = uiState.video
            if (video != null) {
                val channelId = uiState.channelId
                val channelColor = getChannelColor(channelId)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Video thumbnail with play overlay
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        // Thumbnail image from YouTube CDN
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = video.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )

                        // Dark overlay for contrast
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Color.Black.copy(alpha = 0.3f))
                        )

                        // Play button
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                                .clickable {
                                    openYouTubeVideo(video.id)
                                    viewModel.playVideo()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play Video",
                                tint = CharcoalText,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Video Info
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(channelColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = channelId.uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = channelColor
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${video.durationSeconds / 60} minutes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MediumGrayText
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            if (uiState.isCompleted) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.School,
                                        contentDescription = "Learned",
                                        tint = CorrectGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Learned",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = CorrectGreen
                                    )
                                }
                            } else if (uiState.isWatched) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Visibility,
                                        contentDescription = "Watched",
                                        tint = channelColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Watched",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = channelColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SelectionContainer {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Stats Row (views, likes, quiz availability badge)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.Visibility,
                                    contentDescription = "Views",
                                    tint = MediumGrayText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatViewCount(video.viewCount).replace(" views", ""),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MediumGrayText
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.ThumbUp,
                                    contentDescription = "Likes",
                                    tint = MediumGrayText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatLikesCount(video.likeCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MediumGrayText
                                )
                            }

                            if (video.releaseDate.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Event,
                                        contentDescription = "Release Date",
                                        tint = MediumGrayText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = formatDate(video.releaseDate),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MediumGrayText
                                    )
                                }
                            }

                            if (video.questions.isNotEmpty()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Quiz,
                                        contentDescription = "Quiz Available",
                                        tint = channelColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Quiz Available",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = channelColor
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Clickable and Expandable Description
                        var isDescExpanded by remember { mutableStateOf(false) }
                        var showReadMore by remember { mutableStateOf(false) }
                        val annotatedDescription = remember(video.description) {
                            buildAnnotatedDescription(video.description, channelColor)
                        }

                        Column {
                            SelectionContainer {
                                ClickableText(
                                    text = annotatedDescription,
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MediumGrayText),
                                    maxLines = if (isDescExpanded) Int.MAX_VALUE else 5,
                                    overflow = TextOverflow.Ellipsis,
                                    onTextLayout = { textLayoutResult ->
                                        if (!isDescExpanded) {
                                            showReadMore = textLayoutResult.hasVisualOverflow || textLayoutResult.lineCount >= 5
                                        }
                                    },
                                    onClick = { offset ->
                                        val link = annotatedDescription.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()
                                        if (link != null) {
                                            openExternalLink(link.item)
                                        } else {
                                            if (showReadMore) {
                                                isDescExpanded = !isDescExpanded
                                            }
                                        }
                                    }
                                )
                            }
                            if (showReadMore) {
                                Text(
                                    text = if (isDescExpanded) "Show less" else "Show more",
                                    color = channelColor,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { isDescExpanded = !isDescExpanded }
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Test what you learned CTA card
                    if (video.questions.isNotEmpty()) {
                        TestWhatYouLearnedCard(
                            channelColor = channelColor,
                            onClick = {
                                viewModel.onTakeQuiz()
                                onNavigateToQuiz(video.id)
                            }
                        )
                    }

                    // Chapters Section
                    if (video.chapters.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Chapters",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                video.chapters.forEachIndexed { index, chapter ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                openYouTubeVideo(video.id, chapter.startSeconds)
                                                viewModel.playVideo()
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(channelColor.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = formatTimestamp(chapter.startSeconds),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = channelColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = chapter.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = CharcoalText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = "Play Chapter",
                                            tint = MediumGrayText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    if (index < video.chapters.lastIndex) {
                                        HorizontalDivider(
                                            color = CardBorder,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Part of Courses Section
                    if (uiState.playlists.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Part of Courses",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.playlists) { playlist ->
                                PlaylistCarouselCard(
                                    playlist = playlist,
                                    isCourse = playlist.id in uiState.coursePlaylistIds,
                                    channelName = getChannelName(playlist.channelId),
                                    channelColor = getChannelColor(playlist.channelId),
                                    firstVideoThumbnail = playlist.thumbnailUrl,
                                    extraInfo = "${playlist.videoIds.size} videos",
                                    onClick = { onNavigateToCourse(playlist.id) }
                                )
                            }
                        }
                    }

                    // Related Videos Section
                    if (uiState.relatedVideos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Related Videos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.relatedVideos) { relatedVideo ->
                                val relChannelId = relatedVideo.playlistIds.firstOrNull()
                                    ?.let { getChannelIdFromPlaylist(it) } ?: ""
                                VideoCard(
                                    video = relatedVideo,
                                    channelName = getChannelName(relChannelId),
                                    channelColor = getChannelColor(relChannelId),
                                    onClick = { onNavigateToVideo(relatedVideo.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Video not found", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
fun TestWhatYouLearnedCard(
    channelColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(channelColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AssignmentTurnedIn,
                    contentDescription = "Quiz Icon",
                    tint = channelColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Test What You Learned",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Take a quick quiz to cement this lesson",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediumGrayText
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Start Quiz",
                tint = MediumGrayText
            )
        }
    }
}

// Local helper functions

fun formatLikesCount(count: Long): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M"
        count >= 1_000 -> "${count / 1_000}K"
        else -> "$count"
    }
}

fun formatDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    val parts = dateString.split("-")
    if (parts.size != 3) return dateString
    val year = parts[0]
    val month = parts[1].toIntOrNull() ?: return dateString
    val day = parts[2].toIntOrNull() ?: return dateString
    
    val monthName = when (month) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        12 -> "Dec"
        else -> return dateString
    }
    
    return "$day $monthName $year"
}

fun formatTimestamp(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "${m}:${s.toString().padStart(2, '0')}"
    }
}

fun getChannelName(channelId: String): String {
    return when (channelId) {
        "crashcourse" -> "CrashCourse"
        "scishow" -> "SciShow"
        "pbseons" -> "PBS Eons"
        "bizarrebeasts" -> "Bizarre Beasts"
        "studyhall" -> "Study Hall"
        "microcosmos" -> "Journey to the Microcosmos"
        else -> channelId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

fun getChannelIdFromPlaylist(playlistId: String): String {
    val id = playlistId.lowercase()
    return when {
        id.startsWith("crashcourse") || id.startsWith("crash_course") -> "crashcourse"
        id.startsWith("scishow") || id.startsWith("sci_show") -> "scishow"
        id.startsWith("pbseons") || id.startsWith("eo_") || id.startsWith("pbs_eons") -> "pbseons"
        id.startsWith("bizarrebeasts") || id.startsWith("bizarre_beasts") -> "bizarrebeasts"
        id.startsWith("studyhall") || id.startsWith("study_hall") -> "studyhall"
        id.startsWith("microcosmos") || id.startsWith("journey_") -> "microcosmos"
        else -> "complexly"
    }
}

fun buildAnnotatedDescription(description: String, linkColor: Color): AnnotatedString {
    val urlRegex = "(https?://[\\w\\d:#@%/;\$()~_?\\+-=\\\\\\.&!*|,]+)".toRegex()
    return buildAnnotatedString {
        var lastIndex = 0
        urlRegex.findAll(description).forEach { matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1
            
            if (start > lastIndex) {
                append(description.substring(lastIndex, start))
            }
            
            val url = matchResult.value
            pushStringAnnotation(tag = "URL", annotation = url)
            withStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(url)
            }
            pop()
            lastIndex = end
        }
        
        if (lastIndex < description.length) {
            append(description.substring(lastIndex))
        }
    }
}
