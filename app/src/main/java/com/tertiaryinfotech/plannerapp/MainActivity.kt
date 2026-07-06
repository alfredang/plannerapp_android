package com.tertiaryinfotech.plannerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tertiaryinfotech.plannerapp.ui.RootScreen
import com.tertiaryinfotech.plannerapp.ui.theme.PlannerAppTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PlannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlannerAppTheme {
                RootScreen(viewModel)
            }
        }
    }
}
