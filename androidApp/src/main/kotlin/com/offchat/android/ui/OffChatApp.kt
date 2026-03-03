package com.offchat.android.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.offchat.android.ui.screens.ChatScreen
import com.offchat.android.ui.screens.ChatsListScreen
import com.offchat.android.ui.screens.HomeScreen
import com.offchat.android.ui.screens.PeerDiscoveryScreen
import com.offchat.android.ui.screens.RegistrationScreen
import com.offchat.android.ui.screens.SettingsScreen
import com.offchat.android.ui.screens.SplashScreen
import com.offchat.android.ui.theme.OffChatTheme

@Composable
fun OffChatApp() {
    OffChatTheme {
        val context = LocalContext.current
        val prefs = context.getSharedPreferences("offchat_prefs", Context.MODE_PRIVATE)
        val isRegistered = prefs.getBoolean("is_registered", false)

        val navController = rememberNavController()

        // If already registered, skip splash & registration
        val startDestination = if (isRegistered) "home" else "splash"

        NavHost(navController = navController, startDestination = startDestination) {

            composable("splash") {
                SplashScreen(
                    onNavigateToRegistration = {
                        navController.navigate("registration") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }

            composable("registration") {
                RegistrationScreen(
                    onComplete = {
                        navController.navigate("home") {
                            popUpTo("registration") { inclusive = true }
                        }
                    }
                )
            }

            composable("home") {
                // Read user info from SharedPreferences
                val displayName = prefs.getString("user_display_name", "") ?: ""
                val phoneNumber = prefs.getString("user_phone_number", "") ?: ""
                HomeScreen(
                    displayName = displayName,
                    phoneNumber = phoneNumber,
                    onDiscoverClick = { navController.navigate("discovery") },
                    onChatsClick = { navController.navigate("chats") },
                    onSettingsClick = { navController.navigate("settings") }
                )
            }

            // Discovery: scan + connect to peers
            composable("discovery") {
                PeerDiscoveryScreen(
                    onPeerChat = { peerId, peerName ->
                        navController.navigate("chat/$peerId/$peerName")
                    },
                    onSettingsClick = { navController.navigate("settings") },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Chats: chat history list
            composable("chats") {
                ChatsListScreen(
                    onChatClick = { peerId, peerName ->
                        navController.navigate("chat/$peerId/$peerName")
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Chat with a specific peer
            composable(
                route = "chat/{peerId}/{peerName}",
                arguments = listOf(
                    navArgument("peerId") { type = NavType.StringType },
                    navArgument("peerName") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val peerId = backStackEntry.arguments?.getString("peerId") ?: ""
                val peerName = backStackEntry.arguments?.getString("peerName") ?: ""
                ChatScreen(
                    peerId = peerId,
                    peerName = peerName,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(onBackClick = { navController.popBackStack() })
            }
        }
    }
}
