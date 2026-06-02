package com.complexly.academy.ui.discover

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil3.compose.AsyncImage
import com.complexly.academy.data.VideoSort
import com.complexly.academy.model.Channel
import com.complexly.academy.model.Playlist
import com.complexly.academy.model.Video
import com.complexly.academy.ui.home.getChannelColor
import com.complexly.academy.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverScreen(
    viewModel: DiscoverViewModel,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToCourse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    val activeChannel by viewModel.activeChannel.collectAsState()
    val activePlaylist by viewModel.activePlaylist.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Explore",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CreamBackground),
                actions = {
                    // Sort dropdown
                    SortDropdown(currentSort = sort, onSortChanged = viewModel::setSort)
                }
            )
        },
        containerColor = CreamBackground,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            SearchBar(
                query = query,
                onQueryChanged = viewModel::onQueryChanged
            )

            // Channel filter chips
            if (uiState.channels.isNotEmpty()) {
                ChannelFilterRow(
                    channels = uiState.channels,
                    activeChannelId = activeChannel,
                    onChannelSelected = viewModel::setChannelFilter,
                    onClear = viewModel::clearFilters
                )
            }

            // Type filter chips (All / Courses / Playlists)
            TypeFilterRow(currentFilter = typeFilter, onFilterChanged = viewModel::setTypeFilter)

            // Playlist filter chips (shown when a channel is selected)
            val channelPlaylists by viewModel.channelPlaylists.collectAsState()
            if (activeChannel != null && channelPlaylists.isNotEmpty()) {
                PlaylistFilterRow(
                    playlists = channelPlaylists,
                    activePlaylistId = activePlaylist,
                    onPlaylistSelected = viewModel::setPlaylistFilter
                )
            }

            // Matching playlists horizontal carousel
            if (uiState.matchingPlaylists.isNotEmpty()) {
                SectionHeaderSmall(
                    title = "Playlists",
                    icon = Icons.AutoMirrored.Rounded.MenuBook
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.matchingPlaylists) { pl ->
                        PlaylistCard(
                            playlistModel = pl,
                            channelColor = getChannelColor(pl.channelId),
                            onClick = { onNavigateToCourse(pl.playlist.id) }
                        )
                    }
                }
            }

            // Result count / active filter badge
            if (activeChannel != null || activePlaylist != null || query.isNotBlank()) {
                val label = when {
                    activePlaylist != null -> {
                        val pl = uiState.playlists.find { it.id == activePlaylist }
                        "Showing ${pl?.title ?: "playlist"} · ${uiState.searchResults.size} videos"
                    }
                    activeChannel != null -> {
                        val ch = uiState.channels.find { it.id == activeChannel }
                        "Showing ${ch?.title ?: "channel"} · ${uiState.searchResults.size} videos"
                    }
                    query.isNotBlank() -> {
                        "${uiState.searchResults.size} result${if (uiState.searchResults.size != 1) "s" else ""} for \"$query\""
                    }
                    else -> ""
                }
                if (label.isNotEmpty()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGrayText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    if (uiState.searchResults.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val msg = when {
                                    activeChannel != null -> "No videos found for this channel"
                                    query.isNotBlank() -> "No results found for \"$query\""
                                    else -> "No videos available"
                                }
                                Text(
                                    text = msg,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MediumGrayText
                                )
                            }
                        }
                    } else {
                        items(uiState.searchResults) { item ->
                            VideoSearchResultRow(
                                video = item.video,
                                channelId = item.channelId,
                                channelColor = getChannelColor(item.channelId),
                                onClick = { onNavigateToVideo(item.video.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SortDropdown(currentSort: VideoSort, onSortChanged: (VideoSort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sortLabels = mapOf(
        VideoSort.RELEVANCE to "Relevance",
        VideoSort.POPULAR to "Most popular",
        VideoSort.MOST_LIKED to "Most liked",
        VideoSort.NEWEST to "Newest first",
        VideoSort.OLDEST to "Oldest first",
        VideoSort.TITLE_AZ to "Title A→Z",
        VideoSort.TITLE_ZA to "Title Z→A",
        VideoSort.DURATION_SHORT_FIRST to "Shortest first",
        VideoSort.DURATION_LONG_FIRST to "Longest first",
    )

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort", modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(sortLabels[currentSort] ?: "Sort", style = MaterialTheme.typography.labelLarge)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            sortLabels.forEach { (sortType, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (sortType == currentSort) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSortChanged(sortType)
                        expanded = false
                    },
                    trailingIcon = {
                        if (sortType == currentSort) {
                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ChannelFilterRow(
    channels: List<Channel>,
    activeChannelId: String?,
    onChannelSelected: (String?) -> Unit,
    onClear: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // "All" chip
            FilterChip(
                selected = activeChannelId == null,
                onClick = { onClear() },
                label = { Text("All") },
                leadingIcon = if (activeChannelId == null) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null
            )
        }
        items(channels) { channel ->
            val color = getChannelColor(channel.id)
            FilterChip(
                selected = activeChannelId == channel.id,
                onClick = { onChannelSelected(if (activeChannelId == channel.id) null else channel.id) },
                label = { Text(channel.title) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                )
            )
        }
    }
}

@Composable
fun PlaylistFilterRow(
    playlists: List<Playlist>,
    activePlaylistId: String?,
    onPlaylistSelected: (String?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = activePlaylistId == null,
                onClick = { onPlaylistSelected(null) },
                label = { Text("All playlists") },
                leadingIcon = if (activePlaylistId == null) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null
            )
        }
        items(playlists) { playlist ->
            FilterChip(
                selected = activePlaylistId == playlist.id,
                onClick = { onPlaylistSelected(if (activePlaylistId == playlist.id) null else playlist.id) },
                label = { Text(playlist.title) }
            )
        }
    }
}

@Composable
fun TypeFilterRow(
    currentFilter: PlaylistTypeFilter,
    onFilterChanged: (PlaylistTypeFilter) -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val options = listOf(
            PlaylistTypeFilter.ALL to "All",
            PlaylistTypeFilter.COURSES to "Courses",
            PlaylistTypeFilter.PLAYLISTS_ONLY to "Playlists only"
        )
        items(options) { (filter, label) ->
            FilterChip(
                selected = currentFilter == filter,
                onClick = { onFilterChanged(filter) },
                label = { Text(label) },
                leadingIcon = if (currentFilter == filter) {
                    { Icon(Icons.Rounded.Check, contentDescription = null, Modifier.size(18.dp)) }
                } else null
            )
        }
    }
}

/** Compact section header for inline use above horizontal rows. */
@Composable
fun SectionHeaderSmall(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MediumGrayText,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MediumGrayText,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Card for a playlist in the horizontal carousel. */
@Composable
fun PlaylistCard(
    playlistModel: PlaylistUiModel,
    channelColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .clickable { onClick() }
    ) {
        // First video thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            if (playlistModel.firstVideoThumbnailUrl.isNotEmpty()) {
                AsyncImage(
                    model = playlistModel.firstVideoThumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                // Course / Playlist type badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (playlistModel.isCourse) channelColor
                            else Color.Black.copy(alpha = 0.55f)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (playlistModel.isCourse) "COURSE" else "PLAYLIST",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Video count badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${playlistModel.videoCount} videos",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(channelColor.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.MenuBook,
                        contentDescription = null,
                        tint = channelColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Title
        Text(
            text = playlistModel.playlist.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CharcoalText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
        )

        // Channel name
        Text(
            text = playlistModel.channelTitle,
            style = MaterialTheme.typography.labelSmall,
            color = MediumGrayText,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
        )

        // Stats row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text(
                text = playlistModel.formattedDuration,
                style = MaterialTheme.typography.labelSmall,
                color = MediumGrayText
            )
            Spacer(Modifier.width(4.dp))
            if (playlistModel.totalViews > 0) {
                Text(
                    text = "· ${formatViewCount(playlistModel.totalViews)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGrayText
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChanged,
        placeholder = {
            Text(
                text = "Search videos, playlists...",
                style = MaterialTheme.typography.bodyLarge,
                color = MediumGrayText
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search Icon",
                tint = MediumGrayText
            )
        },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun VideoSearchResultRow(
    video: Video,
    channelId: String,
    channelColor: Color,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // YouTube thumbnail
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
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(channelColor.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = channelId.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = channelColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${video.durationSeconds / 60} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGrayText
                    )
                    if (video.viewCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formatViewCount(video.viewCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MediumGrayText
                        )
                    }
                    if (video.questions.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.AssignmentTurnedIn,
                            contentDescription = "Has quiz",
                            tint = channelColor.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/** Formats a view count like "1.2M views" or "47K views". */
fun formatViewCount(count: Long): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M views"
        count >= 1_000 -> "${count / 1_000}K views"
        else -> "$count views"
    }
}
