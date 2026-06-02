package com.complexly.academy.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.complexly.academy.model.Question
import com.complexly.academy.ui.store.StoreLinks
import com.complexly.academy.ui.store.StoreProduct
import com.complexly.academy.ui.theme.*

/**
 * A horizontal pager that cycles through three cards:
 * 1. Review banner (only shown when there are due reviews)
 * 2. Question of the Day
 * 3. Store hero card
 *
 * The review page is conditionally included, so the pager has 2 or 3 pages.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePager(
    dueReviewCount: Int,
    dailyQuestion: Question?,
    onStartReview: () -> Unit,
    onStartQuiz: () -> Unit,
    onOpenStoreLink: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Build the list of pages based on what's available
    val pages = remember(dueReviewCount, dailyQuestion) {
        buildList {
            if (dueReviewCount > 0) {
                add(HomePageType.REVIEW)
            }
            if (dailyQuestion != null) {
                add(HomePageType.DAILY_QUESTION)
            }
            add(HomePageType.STORE_HERO)
        }
    }

    if (pages.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 16.dp
        ) { pageIndex ->
            val page = pages[pageIndex]
            when (page) {
                HomePageType.REVIEW -> {
                    ReviewPagerCard(
                        dueCount = dueReviewCount,
                        onStartReview = onStartReview
                    )
                }
                HomePageType.DAILY_QUESTION -> {
                    if (dailyQuestion != null) {
                        DailyQuestionPagerCard(
                            questionText = dailyQuestion.text,
                            onStartQuiz = onStartQuiz
                        )
                    }
                }
                HomePageType.STORE_HERO -> {
                    StoreHeroPagerCard(
                        onOpenLink = onOpenStoreLink
                    )
                }
            }
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            pages.forEachIndexed { index, _ ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) CharcoalText else MediumGrayText.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

private enum class HomePageType {
    REVIEW,
    DAILY_QUESTION,
    STORE_HERO
}

@Composable
fun ReviewPagerCard(
    dueCount: Int,
    onStartReview: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CharcoalText),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onStartReview() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.School,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$dueCount review${if (dueCount != 1) "s" else ""} due",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Reinforce what you've learned",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DailyQuestionPagerCard(
    questionText: String,
    onStartQuiz: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = EonsAmber),
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onStartQuiz() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "QUESTION OF THE DAY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = questionText,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Tap to answer →",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun StoreHeroPagerCard(
    onOpenLink: (String) -> Unit
) {
    val product = StoreProduct(
        title = "Crash Course Coin",
        subtitle = "Official Crash Course merchandise",
        imageUrl = "https://complexly.store/cdn/shop/files/complexly_icon_748ff6a6-0b09-4797-81a8-bd67ca9f0257.png?crop=center&height=400&v=1757361339&width=400",
        price = null,
        linkUrl = StoreLinks.COMPLEXLY_STORE
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onOpenLink(product.linkUrl) }
    ) {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        ),
                        startY = 60f
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = "complexly.store",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = product.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
