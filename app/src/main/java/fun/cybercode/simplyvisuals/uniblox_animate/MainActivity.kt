package `fun`.cybercode.simplyvisuals.uniblox_animate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import `fun`.cybercode.simplyvisuals.uniblox_animate.ui.StudioAnimator
import `fun`.cybercode.simplyvisuals.uniblox_animate.ui.StudioDashboard
import `fun`.cybercode.simplyvisuals.uniblox_animate.ui.StudioViewModel
import `fun`.cybercode.simplyvisuals.uniblox_animate.ui.theme.StudioTheme
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
                    val viewModel: StudioViewModel = viewModel()
                    var showPermissionDialog by remember { mutableStateOf(!Settings.canDrawOverlays(this@MainActivity)) }

                    if (showPermissionDialog) {
                        AlertDialog(
                            onDismissRequest = { showPermissionDialog = false },
                            title = { Text("Permission Needed") },
                            text = { Text("To enable 'Automatic Crash Recovery' and show the recovery dialog over other apps (like after a device restart), please enable 'Draw over other apps'.\n\nInstructions:\n1. Click 'Go to Settings'.\n2. Find 'Uniblox Animate' in the list.\n3. Toggle 'Allow display over other apps' ON.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showPermissionDialog = false
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                    startActivity(intent)
                                }) {
                                    Text("Go to Settings")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showPermissionDialog = false }) {
                                    Text("Not Now")
                                }
                            }
                        )
                    }
                    
                    LaunchedEffect(Unit) {
                        viewModel.checkAndShowRecovery(this@MainActivity)
                    }

                    StudioApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun StudioApp(viewModel: StudioViewModel) {
    val navController = rememberNavController()
    val recoverySession by viewModel.recoverySession.collectAsState()

    // Handle Recovery Intent or automatic recovery if "Import" clicked in overlay
    // Actually, when overlay clicks "Import", it starts MainActivity with RECOVER=true.
    // In Compose Nav, we can react to this.
    
    NavHost(navController = navController, startDestination = DashboardRoute) {
        composable<DashboardRoute> {
            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? MainActivity
            
            StudioDashboard(
                viewModel = viewModel,
                onOpenAnimator = { sceneId ->
                    navController.navigate(AnimatorRoute(sceneId))
                }
            )
            
            LaunchedEffect(activity?.intent) {
                if (activity?.intent?.getBooleanExtra("RECOVER", false) == true) {
                    val session = viewModel.recoverySession.value
                    if (session != null) {
                        viewModel.selectProject(session.projectId)
                        viewModel.selectScene(session.sceneId)
                        viewModel.selectFrame(session.frameIndex)
                        navController.navigate(AnimatorRoute(session.sceneId))
                    }
                    activity.intent.removeExtra("RECOVER")
                } else if (activity?.intent?.getBooleanExtra("ABANDON", false) == true) {
                    viewModel.clearRecoverySession()
                    activity.intent.removeExtra("ABANDON")
                }
            }
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
