package com.complexly.academy.ui.you

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.complexly.academy.ui.theme.*
import com.complexly.academy.ui.home.getChannelColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouScreen(
    viewModel: YouViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = "Profile Picture",
                            tint = SelectedNavIcon,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = uiState.username,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MediumGrayText
                            )
                        }
                    }
                }
            }

            // Toggles/Preferences
            item {
                Text(
                    text = "Enabled Channels",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            uiState.channels.forEach { (channel, checked) ->
                item {
                    PreferenceToggleRow(
                        title = channel.title,
                        checked = checked,
                        onCheckedChange = { viewModel.toggleChannel(channel.id) },
                        activeColor = getChannelColor(channel.id)
                    )
                }
            }

            item {
                Text(
                    text = "App Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                PreferenceToggleRow(
                    title = "Dark Mode",
                    checked = uiState.darkModeEnabled,
                    onCheckedChange = { viewModel.toggleDarkMode() }
                )
            }
            item {
                PreferenceToggleRow(
                    title = "Push Notifications",
                    checked = uiState.pushNotificationsEnabled,
                    onCheckedChange = { viewModel.togglePushNotifications() }
                )
            }

            // Developer time override section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Developer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MediumGrayText,
                    modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
                )
            }
            item {
                DevTimeOverrideCard(
                    dayOffset = uiState.dayOffset,
                    onAdvanceDay = { viewModel.advanceDay() },
                    onResetDay = { viewModel.resetDay() }
                )
            }
        }
    }
}

@Composable
fun PreferenceToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = SelectedNavIcon
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = activeColor
                )
            )
        }
    }
}

@Composable
fun DevTimeOverrideCard(
    dayOffset: Int,
    onAdvanceDay: () -> Unit,
    onResetDay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Time Override",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (dayOffset == 0) "Using real date" else "Advanced by $dayOffset day${if (dayOffset != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MediumGrayText
                    )
                }
                Row {
                    OutlinedButton(
                        onClick = onResetDay,
                        enabled = dayOffset != 0,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Reset")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onAdvanceDay,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Advance day",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1 Day")
                    }
                }
            }
        }
    }
}
