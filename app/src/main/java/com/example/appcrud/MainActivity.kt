package com.example.appcrud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.appcrud.ui.screens.ItemListScreen
import com.example.appcrud.ui.screens.ItemUpsertScreen
import com.example.appcrud.ui.screens.UserListScreen
import com.example.appcrud.ui.screens.UserUpsertScreen
import com.example.appcrud.ui.theme.AppCRUDTheme
import com.example.appcrud.ui.viewmodel.ItemViewModel
import com.example.appcrud.ui.viewmodel.UserViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppCRUDTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val itemViewModel: ItemViewModel = viewModel()
    val userViewModel: UserViewModel = viewModel()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Items") },
                    selected = currentDestination?.hierarchy?.any { it.route?.startsWith("items") == true } == true,
                    onClick = {
                        navController.navigate("items") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Users") },
                    selected = currentDestination?.hierarchy?.any { it.route?.startsWith("users") == true } == true,
                    onClick = {
                        navController.navigate("users") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "items",
            modifier = Modifier.padding(innerPadding)
        ) {
            // Items Flow
            navigation(startDestination = "items_list", route = "items") {
                composable("items_list") {
                    ItemListScreen(
                        viewModel = itemViewModel,
                        onEditItem = { id -> navController.navigate("items_upsert?id=$id") },
                        onAddItem = { navController.navigate("items_upsert") }
                    )
                }
                composable(
                    route = "items_upsert?id={id}",
                    arguments = listOf(navArgument("id") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")
                    ItemUpsertScreen(
                        viewModel = itemViewModel,
                        itemId = id,
                        onBack = { navController.popBackStack() }
                    )
                }
            }

            // Users Flow
            navigation(startDestination = "users_list", route = "users") {
                composable("users_list") {
                    UserListScreen(
                        viewModel = userViewModel,
                        onEditUser = { id -> navController.navigate("users_upsert?id=$id") },
                        onAddUser = { navController.navigate("users_upsert") }
                    )
                }
                composable(
                    route = "users_upsert?id={id}",
                    arguments = listOf(navArgument("id") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")
                    UserUpsertScreen(
                        viewModel = userViewModel,
                        userId = id,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}