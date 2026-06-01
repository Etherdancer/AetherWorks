package org.example.aetherworks.ui.utilities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.aetherworks.storage.db.dao.RecipeDao
import org.example.aetherworks.storage.db.entity.RecipeEntity
import java.util.UUID

class CookbookViewModel(private val recipeDao: RecipeDao) : ViewModel() {

    val recipes: StateFlow<List<RecipeEntity>> = recipeDao.getAllRecipes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addRecipe(
        title: String,
        description: String,
        ingredients: List<String>,
        steps: List<String>,
        prepTime: Int,
        cookTime: Int
    ) {
        viewModelScope.launch {
            val recipe = RecipeEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                ingredients = ingredients,
                steps = steps,
                prepTimeMinutes = prepTime,
                cookTimeMinutes = cookTime
            )
            withContext(Dispatchers.IO) {
                recipeDao.insertRecipe(recipe)
            }
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                recipeDao.deleteRecipe(recipe)
            }
        }
    }
}
