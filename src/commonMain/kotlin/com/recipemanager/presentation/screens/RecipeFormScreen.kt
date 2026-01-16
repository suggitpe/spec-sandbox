package com.recipemanager.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.recipemanager.domain.model.CookingStep
import com.recipemanager.domain.model.Ingredient
import com.recipemanager.presentation.viewmodel.RecipeFormViewModel
import kotlinx.datetime.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeFormScreen(
    recipeId: String?,
    viewModel: RecipeFormViewModel,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    var showIngredientDialog by remember { mutableStateOf(false) }
    var showStepDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

    LaunchedEffect(recipeId) {
        recipeId?.let { viewModel.loadRecipe(it) }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            onSaveSuccess()
            viewModel.resetSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recipeId != null) "Edit Recipe" else "Create Recipe") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.saveRecipe() },
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Error message
                    state.error?.let { error ->
                        item {
                            ErrorCard(
                                message = error,
                                onDismiss = { viewModel.clearError() }
                            )
                        }
                    }

                    // Basic Information
                    item {
                        Text(
                            text = "Basic Information",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = state.title,
                            onValueChange = { viewModel.updateTitle(it) },
                            label = { Text("Recipe Title *") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = state.validationErrors.containsKey("title"),
                            supportingText = {
                                state.validationErrors["title"]?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = { viewModel.updateDescription(it) },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5
                        )
                    }

                    // Time and Servings
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.preparationTime.toString(),
                                onValueChange = { 
                                    it.toIntOrNull()?.let { time -> viewModel.updatePreparationTime(time) }
                                },
                                label = { Text("Prep (min)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            
                            OutlinedTextField(
                                value = state.cookingTime.toString(),
                                onValueChange = { 
                                    it.toIntOrNull()?.let { time -> viewModel.updateCookingTime(time) }
                                },
                                label = { Text("Cook (min)") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            
                            OutlinedTextField(
                                value = state.servings.toString(),
                                onValueChange = { 
                                    it.toIntOrNull()?.let { servings -> viewModel.updateServings(servings) }
                                },
                                label = { Text("Servings") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    // Tags
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tags",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { showTagDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Tag")
                            }
                        }
                    }

                    if (state.tags.isNotEmpty()) {
                        item {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                state.tags.forEach { tag ->
                                    InputChip(
                                        selected = false,
                                        onClick = { viewModel.removeTag(tag) },
                                        label = { Text(tag) },
                                        trailingIcon = {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Remove",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Ingredients
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Ingredients *",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                state.validationErrors["ingredients"]?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            IconButton(onClick = { showIngredientDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Ingredient")
                            }
                        }
                    }

                    items(state.ingredients, key = { it.id }) { ingredient ->
                        IngredientCard(
                            ingredient = ingredient,
                            onDelete = { viewModel.removeIngredient(ingredient.id) }
                        )
                    }

                    // Cooking Steps
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Cooking Steps *",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                state.validationErrors["steps"]?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            IconButton(onClick = { showStepDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Add Step")
                            }
                        }
                    }

                    itemsIndexed(state.steps.sortedBy { it.stepNumber }, key = { _, step -> step.id }) { index, step ->
                        CookingStepCard(
                            step = step,
                            stepNumber = index + 1,
                            onDelete = { viewModel.removeStep(step.id) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showIngredientDialog) {
        AddIngredientDialog(
            onDismiss = { showIngredientDialog = false },
            onAdd = { ingredient ->
                viewModel.addIngredient(ingredient)
                showIngredientDialog = false
            }
        )
    }

    if (showStepDialog) {
        AddStepDialog(
            stepNumber = state.steps.size + 1,
            onDismiss = { showStepDialog = false },
            onAdd = { step ->
                viewModel.addStep(step)
                showStepDialog = false
            }
        )
    }

    if (showTagDialog) {
        AddTagDialog(
            onDismiss = { showTagDialog = false },
            onAdd = { tag ->
                viewModel.addTag(tag)
                showTagDialog = false
            }
        )
    }
}

@Composable
private fun IngredientCard(
    ingredient: Ingredient,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${ingredient.quantity} ${ingredient.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CookingStepCard(
    step: CookingStep,
    stepNumber: Int,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = stepNumber.toString(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Text(
                    text = step.instruction,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AddIngredientDialog(
    onDismiss: () -> Unit,
    onAdd: (Ingredient) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ingredient") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text("Unit (e.g., cups, grams)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && quantity.isNotBlank() && unit.isNotBlank()) {
                        quantity.toDoubleOrNull()?.let { qty ->
                            onAdd(
                                Ingredient(
                                    id = "ing_${Clock.System.now().toEpochMilliseconds()}",
                                    name = name,
                                    quantity = qty,
                                    unit = unit
                                )
                            )
                        }
                    }
                },
                enabled = name.isNotBlank() && quantity.toDoubleOrNull() != null && unit.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddStepDialog(
    stepNumber: Int,
    onDismiss: () -> Unit,
    onAdd: (CookingStep) -> Unit
) {
    var instruction by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Step $stepNumber") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("Instruction") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration (minutes, optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = temperature,
                    onValueChange = { temperature = it },
                    label = { Text("Temperature (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (instruction.isNotBlank()) {
                        onAdd(
                            CookingStep(
                                id = "step_${Clock.System.now().toEpochMilliseconds()}",
                                stepNumber = stepNumber,
                                instruction = instruction,
                                duration = duration.toIntOrNull(),
                                temperature = temperature.toIntOrNull()
                            )
                        )
                    }
                },
                enabled = instruction.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AddTagDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var tag by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Tag") },
        text = {
            OutlinedTextField(
                value = tag,
                onValueChange = { tag = it },
                label = { Text("Tag") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tag.isNotBlank()) {
                        onAdd(tag)
                    }
                },
                enabled = tag.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ErrorCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
