package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.StudioAnimator
import com.example.ui.StudioDashboard
import com.example.ui.StudioViewModel
import com.example.ui.theme.StudioTheme
import kotlinx.serialization.Serializable

@Serializable
object DashboardRoute

@Serializable
data class AnimatorRoute(val sceneId: Long)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudioTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StudioApp()
                }
            }
        }
    }
}

@Composable
fun StudioApp() {
    val navController = rememberNavController()
    val viewModel: StudioViewModel = viewModel()

    NavHost(navController = navController, startDestination = DashboardRoute) {
        composable<DashboardRoute> {
            StudioDashboard(
                viewModel = viewModel,
                onOpenAnimator = { sceneId ->
                    navController.navigate(AnimatorRoute(sceneId))
                }
            )
        }
        composable<AnimatorRoute> { backStackEntry ->
            val route: AnimatorRoute = backStackEntry.toRoute()
            StudioAnimator(
                viewModel = viewModel,
                sceneId = route.sceneId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
