package com.tertiaryinfotech.plannerapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import com.tertiaryinfotech.plannerapp.PlannerViewModel

private data class Tab(val label: String, val icon: ImageVector)

/** Root navigation. House-style bottom tabs: the app's content first, then Feedback + About. */
@Composable
fun RootScreen(viewModel: PlannerViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Tab("Assistant", Icons.Filled.AutoAwesome),
        Tab("Planner", Icons.Filled.Checklist),
        Tab("Calendar", Icons.Filled.CalendarMonth),
        Tab("Archive", Icons.Filled.Inventory2),
        Tab("Feedback", Icons.AutoMirrored.Filled.Chat),
        Tab("About", Icons.Filled.Info),
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = {
                            Text(t.label, maxLines = 1, softWrap = false, fontSize = 10.sp)
                        },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) {
            when (tab) {
                0 -> AssistantScreen(viewModel)
                1 -> PlannerScreen(viewModel)
                2 -> CalendarScreen(viewModel)
                3 -> ArchiveScreen(viewModel)
                4 -> FeedbackScreen()
                else -> AboutScreen()
            }
        }
    }
}
