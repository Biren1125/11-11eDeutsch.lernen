package com.example.deutschlernen.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.deutschlernen.data.DeutschRepository
import com.example.deutschlernen.model.Achievement
import com.example.deutschlernen.model.PrivilegedRole
import com.example.deutschlernen.model.UserSettings
import com.example.deutschlernen.ui.components.GermanFlagRibbon
import com.example.deutschlernen.ui.components.RoleBadgePill
import com.example.deutschlernen.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: DeutschRepository,
    onBack: () -> Unit
) {
    val settings by repository.userSettings.collectAsState()
    val totalXp by repository.totalXp.collectAsState()
    val streak by repository.streak.collectAsState()
    val achievements by repository.achievements.collectAsState()
    val currentRole by repository.currentRole.collectAsState()

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameText by remember { mutableStateOf(settings.playerName) }

    var showDevAuthDialog by remember { mutableStateOf(false) }
    var devPassInput by remember { mutableStateOf("") }
    var devAuthError by remember { mutableStateOf<String?>(null) }
    var isDevConsoleOpen by remember { mutableStateOf(currentRole != null) }

    var showExportImportDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }

    var showResetConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Profile & Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // Player Profile Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                ) {
                    Column {
                        GermanFlagRibbon()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(18.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(GoldPrimary.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = if (settings.playerName.isNotBlank()) settings.playerName.take(1).uppercase() else "D",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = settings.playerName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = {
                                            editNameText = settings.playerName
                                            showEditNameDialog = true
                                        },
                                        modifier = Modifier.size(32.dp).testTag("edit_player_name_btn")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Name", modifier = Modifier.size(16.dp))
                                    }
                                }

                                Text(
                                    text = "Player ID: ${settings.playerId}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (currentRole != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    RoleBadgePill(role = currentRole!!)
                                }
                            }
                        }
                    }
                }
            }

            // Developer / Privileged Console Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (currentRole != null) GoldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (currentRole != null) GoldPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "👑", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Developer Console",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (currentRole == null) {
                                TextButton(
                                    onClick = { showDevAuthDialog = true },
                                    modifier = Modifier.testTag("unlock_dev_btn")
                                ) {
                                    Text("Unlock")
                                }
                            } else {
                                TextButton(
                                    onClick = { isDevConsoleOpen = !isDevConsoleOpen }
                                ) {
                                    Text(if (isDevConsoleOpen) "Collapse" else "Configure")
                                }
                            }
                        }

                        if (currentRole != null && isDevConsoleOpen) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Assigned Role: ${currentRole?.title}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick preset roles
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                FilterChip(
                                    selected = currentRole?.role == "developer",
                                    onClick = {
                                        repository.assignRole(
                                            PrivilegedRole(
                                                id = "dev",
                                                role = "developer",
                                                label = "Developer",
                                                title = "11:11e Developer",
                                                targetPlayerId = settings.playerId,
                                                unlimitedHearts = true,
                                                badgeColor = "#D6B05E",
                                                allUnlocked = true
                                            )
                                        )
                                    },
                                    label = { Text("Developer", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = currentRole?.title == "Queen of the Game",
                                    onClick = {
                                        repository.assignRole(
                                            PrivilegedRole(
                                                id = "queen",
                                                role = "coDeveloper",
                                                label = "Queen",
                                                title = "Queen of the Game",
                                                targetPlayerId = settings.playerId,
                                                unlimitedHearts = true,
                                                badgeColor = "#FF80AB",
                                                allUnlocked = true
                                            )
                                        )
                                    },
                                    label = { Text("Queen", fontSize = 11.sp) }
                                )
                                FilterChip(
                                    selected = currentRole?.role == "elite",
                                    onClick = {
                                        repository.assignRole(
                                            PrivilegedRole(
                                                id = "elite",
                                                role = "elite",
                                                label = "Elite",
                                                title = "Elite German Master",
                                                targetPlayerId = settings.playerId,
                                                unlimitedHearts = true,
                                                badgeColor = "#4AA3A2",
                                                allUnlocked = true
                                            )
                                        )
                                    },
                                    label = { Text("Elite", fontSize = 11.sp) }
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Infinite Hearts (∞)", fontSize = 13.sp)
                                Switch(
                                    checked = currentRole?.unlimitedHearts == true,
                                    onCheckedChange = { checked ->
                                        currentRole?.let {
                                            repository.assignRole(it.copy(unlimitedHearts = checked))
                                        }
                                    }
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Unlock All Levels & Chapters", fontSize = 13.sp)
                                Switch(
                                    checked = currentRole?.allUnlocked == true,
                                    onCheckedChange = { checked ->
                                        currentRole?.let {
                                            repository.assignRole(it.copy(allUnlocked = checked))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Audio & Experience Settings
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Audio & Pronunciation",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Game Sound FX", fontSize = 14.sp)
                            Switch(
                                checked = settings.soundEnabled,
                                onCheckedChange = { repository.updateSettings(settings.copy(soundEnabled = it)) }
                            )
                        }

                        if (settings.soundEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sound Volume: ${(settings.soundVolume * 100).toInt()}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Slider(
                                value = settings.soundVolume,
                                onValueChange = { repository.updateSettings(settings.copy(soundVolume = it)) },
                                valueRange = 0f..1f
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("German Speech Rate: ${String.format("%.1f", settings.ttsSpeed)}x", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Slider(
                            value = settings.ttsSpeed,
                            onValueChange = { repository.updateSettings(settings.copy(ttsSpeed = it)) },
                            valueRange = 0.5f..1.8f
                        )
                    }
                }
            }

            // Achievements Gallery
            item {
                Text(
                    text = "Achievements (${achievements.count { it.unlocked }} / ${achievements.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            items(achievements.size) { index ->
                val ach = achievements[index]
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (ach.unlocked) GoldPrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (ach.unlocked) GoldPrimary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = ach.icon,
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ach.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (ach.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = ach.desc,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (ach.unlocked && ach.unlockedAt != null) {
                                Text(
                                    text = "Unlocked: ${ach.unlockedAt}",
                                    fontSize = 10.sp,
                                    color = GoldPrimary
                                )
                            }
                        }
                        if (ach.unlocked) {
                            Text(text = "✓", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Data Backup & Reset Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Data & Backup",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    backupJsonText = repository.exportBackupJson()
                                    showExportImportDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Backup / Sync", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showResetConfirmDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RedMistake),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reset All", fontSize = 12.sp, color = InkBlack)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Name Dialog
    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit Player Nickname") },
            text = {
                OutlinedTextField(
                    value = editNameText,
                    onValueChange = { editNameText = it },
                    label = { Text("Nickname") },
                    modifier = Modifier.fillMaxWidth().testTag("nickname_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameText.isNotBlank()) {
                            repository.updateSettings(settings.copy(playerName = editNameText.trim()))
                            showEditNameDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Developer Authentication Dialog
    if (showDevAuthDialog) {
        AlertDialog(
            onDismissRequest = { showDevAuthDialog = false },
            title = { Text("Developer Verification") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter the Developer Key to unlock developer privileges, unlimited hearts, and role assignments.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = devPassInput,
                        onValueChange = {
                            devPassInput = it
                            devAuthError = null
                        },
                        label = { Text("Passkey") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("dev_passkey_input")
                    )
                    if (devAuthError != null) {
                        Text(text = devAuthError!!, color = RedMistake, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = repository.authenticateDeveloper(devPassInput.trim())
                        if (success) {
                            devPassInput = ""
                            showDevAuthDialog = false
                            isDevConsoleOpen = true
                        } else {
                            devAuthError = "Invalid developer passkey."
                        }
                    },
                    modifier = Modifier.testTag("verify_dev_pass_btn")
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevAuthDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Export / Import Backup Dialog
    if (showExportImportDialog) {
        AlertDialog(
            onDismissRequest = { showExportImportDialog = false },
            title = { Text("Backup & Data Sync") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Copy this JSON to back up or paste a previous backup JSON to restore your progress.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = backupJsonText,
                        onValueChange = { backupJsonText = it },
                        minLines = 6,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth().testTag("backup_json_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.importBackupJson(backupJsonText)
                        showExportImportDialog = false
                    }
                ) {
                    Text("Restore from JSON")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportImportDialog = false }) { Text("Close") }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset Progress?") },
            text = { Text("Are you sure you want to reset all learning stats, XP, streaks, and history? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        repository.resetAllProgress()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedMistake)
                ) {
                    Text("Yes, Reset Everything", color = InkBlack)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }
}
