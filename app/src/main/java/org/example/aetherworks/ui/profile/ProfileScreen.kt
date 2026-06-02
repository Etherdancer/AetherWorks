package org.example.aetherworks.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.aetherworks.crypto.KeyManager
import org.example.aetherworks.persona.PersonaAgent
import org.example.aetherworks.persona.Profile
import org.example.aetherworks.persona.ProfileField
import org.example.aetherworks.persona.VisibilityLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(modifier: Modifier = Modifier, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val personaAgent = remember { PersonaAgent(context, KeyManager(context)) }
    
    val profile = remember { personaAgent.getProfile() }

    var alias by remember { mutableStateOf(profile?.alias ?: "") }
    
    // About
    var about by remember { mutableStateOf(profile?.about?.value ?: "") }
    var aboutVis by remember { mutableStateOf(profile?.about?.visibility ?: VisibilityLevel.PRIVATE) }
    var guidingPrinciple by remember { mutableStateOf(profile?.guidingPrinciple?.value ?: "") }
    var guidingPrincipleVis by remember { mutableStateOf(profile?.guidingPrinciple?.visibility ?: VisibilityLevel.PRIVATE) }
    
    // Favorites
    var favMusic by remember { mutableStateOf(profile?.favoriteMusic?.value ?: "") }
    var favMusicVis by remember { mutableStateOf(profile?.favoriteMusic?.visibility ?: VisibilityLevel.PRIVATE) }
    var favMovies by remember { mutableStateOf(profile?.favoriteMovies?.value ?: "") }
    var favMoviesVis by remember { mutableStateOf(profile?.favoriteMovies?.visibility ?: VisibilityLevel.PRIVATE) }
    
    // Interests
    var interests by remember { mutableStateOf(profile?.interests?.value ?: "") }
    var interestsVis by remember { mutableStateOf(profile?.interests?.visibility ?: VisibilityLevel.PRIVATE) }

    // Demographics
    var occupation by remember { mutableStateOf(profile?.occupation?.value ?: "") }
    var occupationVis by remember { mutableStateOf(profile?.occupation?.visibility ?: VisibilityLevel.PRIVATE) }
    var relationship by remember { mutableStateOf(profile?.relationshipStatus?.value ?: "") }
    var relationshipVis by remember { mutableStateOf(profile?.relationshipStatus?.visibility ?: VisibilityLevel.PRIVATE) }

    // Astrology
    var westernZodiac by remember { mutableStateOf(profile?.westernZodiac?.value ?: "") }
    var westernZodiacVis by remember { mutableStateOf(profile?.westernZodiac?.visibility ?: VisibilityLevel.PRIVATE) }
    var celticHoroscope by remember { mutableStateOf(profile?.celticHoroscope?.value ?: "") }
    var celticHoroscopeVis by remember { mutableStateOf(profile?.celticHoroscope?.visibility ?: VisibilityLevel.PRIVATE) }
    var chineseZodiac by remember { mutableStateOf(profile?.chineseZodiac?.value ?: "") }
    var chineseZodiacVis by remember { mutableStateOf(profile?.chineseZodiac?.visibility ?: VisibilityLevel.PRIVATE) }
    var mayanKin by remember { mutableStateOf(profile?.mayanKin?.value ?: "") }
    var mayanKinVis by remember { mutableStateOf(profile?.mayanKin?.visibility ?: VisibilityLevel.PRIVATE) }
    var vedicRasi by remember { mutableStateOf(profile?.vedicRasi?.value ?: "") }
    var vedicRasiVis by remember { mutableStateOf(profile?.vedicRasi?.visibility ?: VisibilityLevel.PRIVATE) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile Persona") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        val newProfile = Profile(
                            publicKeyBase64 = personaAgent.publicKeyBase64,
                            alias = alias,
                            avatarPath = profile?.avatarPath,
                            avatarId = profile?.avatarId ?: 0,
                            about = if (about.isNotBlank()) ProfileField(about, aboutVis) else null,
                            guidingPrinciple = if (guidingPrinciple.isNotBlank()) ProfileField(guidingPrinciple, guidingPrincipleVis) else null,
                            favoriteMusic = if (favMusic.isNotBlank()) ProfileField(favMusic, favMusicVis) else null,
                            favoriteMovies = if (favMovies.isNotBlank()) ProfileField(favMovies, favMoviesVis) else null,
                            interests = if (interests.isNotBlank()) ProfileField(interests, interestsVis) else null,
                            occupation = if (occupation.isNotBlank()) ProfileField(occupation, occupationVis) else null,
                            relationshipStatus = if (relationship.isNotBlank()) ProfileField(relationship, relationshipVis) else null,
                            westernZodiac = if (westernZodiac.isNotBlank()) ProfileField(westernZodiac, westernZodiacVis) else null,
                            chineseZodiac = if (chineseZodiac.isNotBlank()) ProfileField(chineseZodiac, chineseZodiacVis) else null,
                            celticHoroscope = if (celticHoroscope.isNotBlank()) ProfileField(celticHoroscope, celticHoroscopeVis) else null,
                            mayanKin = if (mayanKin.isNotBlank()) ProfileField(mayanKin, mayanKinVis) else null,
                            vedicRasi = if (vedicRasi.isNotBlank()) ProfileField(vedicRasi, vedicRasiVis) else null
                        )
                        personaAgent.saveProfile(newProfile)
                        onNavigateBack()
                    }
                }
            ) {
                Text("Save Persona")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Core Identity", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Display Alias (Required)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                Text("About", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                ProfileFieldInput("Bio", about, { about = it }, aboutVis, { aboutVis = it })
                ProfileFieldInput("Guiding Principle", guidingPrinciple, { guidingPrinciple = it }, guidingPrincipleVis, { guidingPrincipleVis = it })
            }
            
            item {
                Text("Favorites & Interests", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                ProfileFieldInput("Favorite Music", favMusic, { favMusic = it }, favMusicVis, { favMusicVis = it })
                ProfileFieldInput("Favorite Movies", favMovies, { favMovies = it }, favMoviesVis, { favMoviesVis = it })
                ProfileFieldInput("Interests (Keywords)", interests, { interests = it }, interestsVis, { interestsVis = it })
            }
            
            item {
                Text("Fictional Demographics", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                ProfileFieldInput("Occupation / Role", occupation, { occupation = it }, occupationVis, { occupationVis = it })
                ProfileFieldInput("Relationship Status", relationship, { relationship = it }, relationshipVis, { relationshipVis = it })
            }
            
            item {
                Text("Astrology & Signs", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                ProfileFieldInput("Western Zodiac", westernZodiac, { westernZodiac = it }, westernZodiacVis, { westernZodiacVis = it })
                ProfileFieldInput("Chinese Zodiac", chineseZodiac, { chineseZodiac = it }, chineseZodiacVis, { chineseZodiacVis = it })
                ProfileFieldInput("Celtic Horoscope", celticHoroscope, { celticHoroscope = it }, celticHoroscopeVis, { celticHoroscopeVis = it })
                ProfileFieldInput("Mayan Kin", mayanKin, { mayanKin = it }, mayanKinVis, { mayanKinVis = it })
                ProfileFieldInput("Vedic Rasi", vedicRasi, { vedicRasi = it }, vedicRasiVis, { vedicRasiVis = it })
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                Text("Network & Relay Settings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("Maximum local storage for relaying encrypted messages from other users.", style = MaterialTheme.typography.bodySmall)
                
                val prefs = context.getSharedPreferences("aether_settings", android.content.Context.MODE_PRIVATE)
                var relayQuotaMb by remember { mutableStateOf(prefs.getInt("relay_quota_mb", 500)) }
                
                val options = listOf(50, 100, 500, 1024, 2048, 5120)
                val labels = listOf("50 MB", "100 MB", "500 MB (Default)", "1 GB", "2 GB", "5 GB")
                
                var expandedQuota by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    Button(onClick = { expandedQuota = true }, modifier = Modifier.fillMaxWidth()) {
                        val currentLabel = labels[options.indexOf(relayQuotaMb).takeIf { it >= 0 } ?: 2]
                        Text("Relay Storage Quota: $currentLabel")
                    }
                    DropdownMenu(
                        expanded = expandedQuota,
                        onDismissRequest = { expandedQuota = false }
                    ) {
                        options.forEachIndexed { index, size ->
                            DropdownMenuItem(
                                text = { Text(labels[index]) },
                                onClick = {
                                    relayQuotaMb = size
                                    prefs.edit().putInt("relay_quota_mb", size).apply()
                                    expandedQuota = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                Text("About & Support", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text("For bug reports and support, please contact:", style = MaterialTheme.typography.bodyMedium)
                Text("etherdancer.zero553@aleeas.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }
}

@Composable
fun ProfileFieldInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visibility: VisibilityLevel,
    onVisibilityChange: (VisibilityLevel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                label = { Text(label) },
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            
            Box {
                Button(onClick = { expanded = true }) {
                    Text(visibility.name.lowercase().capitalize())
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Private") },
                        onClick = { onVisibilityChange(VisibilityLevel.PRIVATE); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Trusted") },
                        onClick = { onVisibilityChange(VisibilityLevel.TRUSTED); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Public") },
                        onClick = { onVisibilityChange(VisibilityLevel.PUBLIC); expanded = false }
                    )
                }
            }
        }
    }
}
