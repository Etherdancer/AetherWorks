package org.example.aetherworks.ui.utilities

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import org.example.aetherworks.storage.db.entity.RecipeEntity
import org.example.aetherworks.ui.components.SharingConfirmationDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookbookScreen(
    modifier: Modifier = Modifier,
    viewModel: CookbookViewModel
) {
    val recipes by viewModel.recipes.collectAsState()
    var selectedRecipe by remember { mutableStateOf<RecipeEntity?>(null) }
    var isCookingMode by remember { mutableStateOf(false) }

    if (selectedRecipe != null) {
        if (isCookingMode) {
            CookingModeScreen(
                recipe = selectedRecipe!!,
                onBack = { isCookingMode = false }
            )
        } else {
            RecipeDetailScreen(
                recipe = selectedRecipe!!,
                onBack = { selectedRecipe = null },
                onStartCooking = { isCookingMode = true }
            )
        }
    } else {
        RecipeListScreen(
            recipes = recipes,
            onRecipeClick = { selectedRecipe = it },
            onDeleteRecipe = { viewModel.deleteRecipe(it) },
            onAddDummyRecipe = {
                // Add a dummy recipe for testing
                viewModel.addRecipe(
                    title = "Classic Spaghetti Carbonara",
                    description = "A classic Italian pasta dish.",
                    ingredients = listOf("400g Spaghetti", "150g Pancetta", "2 large eggs", "50g Pecorino cheese", "50g Parmesan", "Black pepper"),
                    steps = listOf(
                        "Boil the pasta in salted water.",
                        "Fry the pancetta until crisp.",
                        "Beat eggs with cheese and pepper.",
                        "Mix hot pasta with pancetta, then off heat quickly stir in the egg mixture.",
                        "Serve immediately."
                    ),
                    prepTime = 10,
                    cookTime = 15
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    recipes: List<RecipeEntity>,
    onRecipeClick: (RecipeEntity) -> Unit,
    onDeleteRecipe: (RecipeEntity) -> Unit,
    onAddDummyRecipe: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Offline Cookbook") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDummyRecipe) {
                Icon(Icons.Default.Add, contentDescription = "Add Recipe")
            }
        }
    ) { padding ->
        if (recipes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No recipes yet. Tap + to add a dummy recipe.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recipes) { recipe ->
                    Card(onClick = { onRecipeClick(recipe) }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                                Text("${recipe.prepTimeMinutes + recipe.cookTimeMinutes} mins", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { onDeleteRecipe(recipe) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: RecipeEntity,
    onBack: () -> Unit,
    onStartCooking: () -> Unit
) {
    var showShareDialog by remember { mutableStateOf(false) }

    if (showShareDialog) {
        SharingConfirmationDialog(
            onConfirm = {
                // TODO: Execute actual P2P broadcast/share logic
                showShareDialog = false
            },
            onDismiss = { showShareDialog = false },
            title = "Share Recipe Publicly?"
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareDialog = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Publicly")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartCooking,
                icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = null) },
                text = { Text("Start Cooking") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Text(recipe.description, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Ingredients", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(recipe.ingredients) { ingredient ->
                Text("• $ingredient", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Steps", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(recipe.steps.withIndex().toList()) { (index, step) ->
                Text("${index + 1}. $step", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingModeScreen(
    recipe: RecipeEntity,
    onBack: () -> Unit
) {
    val view = LocalView.current
    var currentStep by remember { mutableStateOf(0) }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cooking: ${recipe.title}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Step ${currentStep + 1} of ${recipe.steps.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = recipe.steps[currentStep],
                style = MaterialTheme.typography.headlineMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(64.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { if (currentStep > 0) currentStep-- },
                    enabled = currentStep > 0
                ) {
                    Text("Previous")
                }
                Button(
                    onClick = { if (currentStep < recipe.steps.size - 1) currentStep++ else onBack() }
                ) {
                    Text(if (currentStep < recipe.steps.size - 1) "Next" else "Finish")
                }
            }
        }
    }
}
