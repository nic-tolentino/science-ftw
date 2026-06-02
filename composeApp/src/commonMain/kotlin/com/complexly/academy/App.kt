package com.complexly.academy

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.complexly.academy.data.ContentRepository
import com.complexly.academy.data.UserRepository
import com.complexly.academy.di.AppComponent
import com.complexly.academy.ui.course.CourseDetailsScreen
import com.complexly.academy.ui.course.CourseDetailsViewModel
import com.complexly.academy.ui.discover.DiscoverScreen
import com.complexly.academy.ui.discover.DiscoverViewModel
import com.complexly.academy.ui.home.HomeScreen
import com.complexly.academy.ui.home.HomeViewModel
import com.complexly.academy.ui.navigation.CourseDetailsRoute
import com.complexly.academy.ui.navigation.MainTabsRoute
import com.complexly.academy.ui.navigation.QuizRoute
import com.complexly.academy.ui.navigation.ReviewRoute
import com.complexly.academy.ui.navigation.VideoPlayerRoute
import com.complexly.academy.ui.progress.ProgressScreen
import com.complexly.academy.ui.progress.ProgressViewModel
import com.complexly.academy.ui.quiz.QuizScreen
import com.complexly.academy.ui.quiz.QuizViewModel
import androidx.compose.ui.graphics.Color
import com.complexly.academy.ui.theme.*
import com.complexly.academy.ui.video.VideoPlayerScreen
import com.complexly.academy.ui.video.VideoPlayerViewModel
import com.complexly.academy.ui.you.YouScreen
import com.complexly.academy.ui.you.YouViewModel
import com.complexly.academy.ui.store.StoreScreen
import com.complexly.academy.ui.review.ReviewScreen
import com.complexly.academy.ui.review.ReviewViewModel
import com.complexly.academy.platform.openExternalLink

@Composable
fun App() {
    val contentRepository = remember { AppComponent.instance.contentRepository }
    val userRepository = remember { AppComponent.instance.userRepository }

    ScienceTheme {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = MainTabsRoute,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<MainTabsRoute> {
                MainTabsScreen(
                    contentRepository = contentRepository,
                    userRepository = userRepository,
                    onNavigateToCourse = { courseId ->
                        navController.navigate(CourseDetailsRoute(courseId))
                    },
                    onNavigateToQuiz = { videoId ->
                        navController.navigate(QuizRoute(videoId))
                    },
                    onNavigateToVideo = { videoId ->
                        navController.navigate(VideoPlayerRoute(videoId))
                    },
                    onNavigateToReview = {
                        navController.navigate(ReviewRoute)
                    }
                )
            }

            composable<CourseDetailsRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<CourseDetailsRoute>()
                val courseViewModel = viewModel {
                    CourseDetailsViewModel(route.courseId, contentRepository, userRepository)
                }
                CourseDetailsScreen(
                    viewModel = courseViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToVideo = { videoId, playlistId ->
                        navController.navigate(VideoPlayerRoute(videoId, playlistId))
                    }
                )
            }

            composable<VideoPlayerRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<VideoPlayerRoute>()
                val videoViewModel = viewModel {
                    VideoPlayerViewModel(route.videoId, route.playlistId, contentRepository, userRepository)
                }
                VideoPlayerScreen(
                    viewModel = videoViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToQuiz = { videoId ->
                        navController.navigate(QuizRoute(videoId))
                    },
                    onNavigateToCourse = { courseId ->
                        navController.navigate(CourseDetailsRoute(courseId))
                    },
                    onNavigateToVideo = { videoId ->
                        navController.navigate(VideoPlayerRoute(videoId))
                    }
                )
            }

            composable<QuizRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<QuizRoute>()
                val quizViewModel = viewModel {
                    QuizViewModel(route.videoId, contentRepository, userRepository)
                }
                QuizScreen(
                    viewModel = quizViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<ReviewRoute> {
                val reviewViewModel = viewModel { ReviewViewModel(userRepository, contentRepository) }
                ReviewScreen(
                    viewModel = reviewViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabsScreen(
    contentRepository: ContentRepository,
    userRepository: UserRepository,
    onNavigateToCourse: (String) -> Unit,
    onNavigateToQuiz: (String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToReview: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val discoverViewModel = viewModel { DiscoverViewModel(contentRepository, userRepository) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val safeBottomInset = if (bottomInset > 0.dp) bottomInset else 16.dp
            val customWindowInsets = WindowInsets(bottom = safeBottomInset)

            Column {
                HorizontalDivider(color = CardBorder, thickness = 1.dp)
                NavigationBar(
                    modifier = Modifier.height(72.dp + safeBottomInset),
                    windowInsets = customWindowInsets,
                    containerColor = CreamBackground,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = SelectedNavIcon, indicatorColor = BottomNavIndicator)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Rounded.Search, contentDescription = "Discover") },
                        label = { Text("Discover") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = SelectedNavIcon, indicatorColor = BottomNavIndicator)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = "Progress") },
                        label = { Text("Progress") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = SelectedNavIcon, indicatorColor = BottomNavIndicator)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Rounded.ShoppingBag, contentDescription = "Store") },
                        label = { Text("Store") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = SelectedNavIcon, indicatorColor = BottomNavIndicator)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 4,
                        onClick = { selectedTab = 4 },
                        icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = SelectedNavIcon, indicatorColor = BottomNavIndicator)
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> {
                val homeViewModel = viewModel { HomeViewModel(contentRepository, userRepository, AppComponent.instance.getRecentlyViewedUseCase) }
                val dueCount = userRepository.getDueReviews().size
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToCourse = onNavigateToCourse,
                    onNavigateToQuiz = onNavigateToQuiz,
                    onNavigateToVideo = onNavigateToVideo,
                    onChannelClick = { channelId ->
                        selectedTab = 1
                        discoverViewModel.setChannelFilter(channelId)
                    },
                    onStartReview = onNavigateToReview,
                    dueReviewCount = dueCount,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            1 -> {
                DiscoverScreen(
                    viewModel = discoverViewModel,
                    onNavigateToVideo = onNavigateToVideo,
                    onNavigateToCourse = onNavigateToCourse,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            2 -> {
                val progressViewModel = viewModel { ProgressViewModel(userRepository, contentRepository) }
                ProgressScreen(
                    viewModel = progressViewModel,
                    onNavigateToVideo = onNavigateToVideo,
                    onNavigateToCourse = onNavigateToCourse,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            3 -> {
                StoreScreen(
                    onOpenLink = { url -> openExternalLink(url) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            4 -> {
                val youViewModel = viewModel { YouViewModel(contentRepository, userRepository) }
                YouScreen(
                    viewModel = youViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
