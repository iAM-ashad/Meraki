package com.iamashad.meraki.screens.journal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamashad.meraki.model.Journal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddJournalScreen(
    viewModel: JournalViewModel,
    userId: String,
    journalId: String,
    onClose: () -> Unit,
    onSave: () -> Unit
) {
    val bottomSheetState = rememberBottomSheetScaffoldState(
        bottomSheetState = SheetState(
            initialValue = SheetValue.Expanded,
            skipHiddenState = true,
            skipPartiallyExpanded = false,
            density = Density(1f)
        )
    )

    var step by remember { mutableIntStateOf(1) }
    var selectedEmotions by remember { mutableStateOf(listOf<String>()) }
    var selectedReasons by remember { mutableStateOf(listOf<String>()) }
    var journalEntry by remember { mutableStateOf("") }
    var moodScore by remember { mutableIntStateOf(50) }

    BottomSheetScaffold(
        scaffoldState = bottomSheetState,
        sheetSwipeEnabled = false,
        sheetPeekHeight = 100.dp,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                when (step) {
                    1 -> EmotionSelectionSheet(
                        selectedEmotions = selectedEmotions,
                        onEmotionsSelected = {
                            selectedEmotions = it
                            moodScore = calculateMoodScore(it)
                        },
                        onNext = { step = 2 },
                        onClose = onClose
                    )

                    2 -> ReasonSelectionSheet(
                        selectedReasons = selectedReasons,
                        onReasonsSelected = { selectedReasons = it },
                        onNext = { step = 3 },
                        onClose = onClose
                    )

                    3 -> JournalEntrySheet(
                        journalEntry = journalEntry,
                        onJournalEntryChanged = { journalEntry = it },
                        onSave = {
                            viewModel.addJournal(
                                Journal(
                                    journalId = journalId,
                                    userId = userId,
                                    title = selectedEmotions.joinToString(),
                                    content = journalEntry,
                                    moodScore = moodScore,
                                    reasons = selectedReasons,
                                    date = System.currentTimeMillis()
                                )
                            )
                            onSave()
                            onClose()
                        },
                        onClose = onClose
                    )
                }
            }
        },
        sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Placeholder content for the main scaffold area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.inversePrimary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmotionSelectionSheet(
    selectedEmotions: List<String>,
    onEmotionsSelected: (List<String>) -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val allEmotions = listOf(
        "Happy" to "😊",
        "Sad" to "😢",
        "Excited" to "🤩",
        "Calm" to "😌",
        "Confused" to "😕",
        "Surprised" to "😲",
        "Amazed" to "😮",
        "Peaceful" to "🕊️",
        "Cool" to "😎",
        "Stressed" to "😣",
        "Angry" to "😡",
        "Lonely" to "🥺",
        "Grateful" to "🙏",
        "Hopeful" to "🌟",
        "Tired" to "😴",
        "Awkward" to "😅"
    )

    var searchQuery by remember { mutableStateOf("") }
    val filteredEmotions = allEmotions.filter {
        it.first.contains(searchQuery, ignoreCase = true)
    }

    val commonlyUsed = listOf(
        "Confused" to "😕",
        "Excited" to "🤩",
        "Cool" to "😎",
        "Surprised" to "😲",
        "Peaceful" to "🕊️",
        "Amazed" to "😮"
    )

    var selected by remember { mutableStateOf(selectedEmotions) }

    SheetLayout(
        title = "Choose the emotions that match your mood",
        onClose = onClose,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp) // Leave space for the floating button
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search emotions") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search Icon")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                Text(
                    text = "Commonly Used",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(commonlyUsed) { (emotionName, emoji) ->
                        EmotionChip(
                            emotionName = emotionName,
                            emoji = emoji,
                            isSelected = selected.contains(emotionName),
                            onClick = {
                                selected =
                                    if (selected.contains(emotionName)) selected - emotionName else selected + emotionName
                                onEmotionsSelected(selected)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "All Emotions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(filteredEmotions) { (emotionName, emoji) ->
                        EmotionChip(
                            emotionName = emotionName,
                            emoji = emoji,
                            isSelected = selected.contains(emotionName),
                            onClick = {
                                selected =
                                    if (selected.contains(emotionName)) selected - emotionName else selected + emotionName
                                onEmotionsSelected(selected)
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = onNext,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        top = 16.dp,
                        bottom = 4.dp,
                        end = 12.dp,
                        start = 16.dp
                    )
                    .clip(CircleShape)
            ) {
                Text(
                    "→",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}


@Composable
fun EmotionChip(
    emotionName: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .size(80.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = emotionName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReasonSelectionSheet(
    selectedReasons: List<String>,
    onReasonsSelected: (List<String>) -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val commonlyUsedReasons = listOf("Family", "Self-esteem", "Sleep", "Social")

    val allReasons = listOf(
        "Family",
        "Work",
        "Hobbies",
        "Weather",
        "Love",
        "Sleep",
        "Breakup",
        "Social",
        "Food",
        "Party",
        "Self-esteem",
        "Wife",
        "Friends",
        "Health",
        "Career",
        "Exercise"
    )

    var selected by remember { mutableStateOf(selectedReasons) }

    SheetLayout(
        title = "What's the reason making you feel this way?",
        onClose = onClose
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp) // Leave space for the floating button
            ) {
                // Commonly Used Reasons Section
                Text(
                    text = "Commonly Used Reasons",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(commonlyUsedReasons) { reason ->
                        ReasonChip(
                            reason = reason,
                            isSelected = selected.contains(reason),
                            onClick = {
                                selected =
                                    if (selected.contains(reason)) selected - reason else selected + reason
                                onReasonsSelected(selected)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "All Reasons",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(allReasons) { reason ->
                        ReasonChip(
                            reason = reason,
                            isSelected = selected.contains(reason),
                            onClick = {
                                selected =
                                    if (selected.contains(reason)) selected - reason else selected + reason
                                onReasonsSelected(selected)
                            }
                        )
                    }
                }
            }

            // Floating Action Button to Navigate to Next Screen
            FloatingActionButton(
                onClick = onNext,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        top = 16.dp,
                        bottom = 4.dp,
                        end = 12.dp,
                        start = 16.dp
                    )
                    .clip(CircleShape)
            ) {
                Text(
                    "→",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun ReasonChip(
    reason: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Text(
            text = reason,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
        )
    }
}


@Composable
fun JournalEntrySheet(
    journalEntry: String,
    onJournalEntryChanged: (String) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    SheetLayout(
        title = "Any thing you want to add",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Instructions
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Add notes reflecting on your mood",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Text input with placeholder
            OutlinedTextField(
                value = journalEntry,
                onValueChange = onJournalEntryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = {
                    Text(
                        text = "Start writing here...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                colors = TextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.inverseSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.background
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = false,
                maxLines = 10
            )

            // Save Button
            Button(
                onClick = onSave,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp)
                    .fillMaxWidth(.6f), // Make the button smaller
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}


@Composable
fun SheetLayout(
    title: String,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.inversePrimary
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

fun calculateMoodScore(selectedEmotions: List<String>): Int {
    if (selectedEmotions.isEmpty()) return 50

    val emotionScores = mapOf(
        "Happy" to 90,
        "Sad" to 25,
        "Excited" to 80,
        "Calm" to 70,
        "Confused" to 40,
        "Surprised" to 50,
        "Amazed" to 85,
        "Peaceful" to 75,
        "Cool" to 60,
        "Stressed" to 30,
        "Angry" to 20,
        "Lonely" to 35,
        "Grateful" to 95,
        "Hopeful" to 80,
        "Tired" to 40,
        "Awkward" to 45
    )

    val totalScore = selectedEmotions.sumOf { emotionScores[it] ?: 50 }
    return totalScore / selectedEmotions.size
}
