package com.iamashad.meraki.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                    brush =  Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Preparing Journal Screen...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
fun EmotionSelectionSheet(
    selectedEmotions: List<String>,
    onEmotionsSelected: (List<String>) -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val emotions = listOf("Happy", "Sad", "Excited", "Calm", "Confused", "Surprised")
    var selected by remember { mutableStateOf(selectedEmotions) }

    SheetLayout(
        title = "Choose emotions",
        onClose = onClose
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(emotions) { emotion ->
                OutlinedButton(
                    onClick = {
                        selected =
                            if (selected.contains(emotion)) selected - emotion else selected + emotion
                        onEmotionsSelected(selected)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected.contains(emotion)) Color.LightGray else Color.Transparent
                    )
                ) {
                    Text(text = emotion, fontSize = 16.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNext, modifier = Modifier.align(Alignment.End)) {
            Text("Next")
        }
    }
}

@Composable
fun ReasonSelectionSheet(
    selectedReasons: List<String>,
    onReasonsSelected: (List<String>) -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    val reasons = listOf("Family", "Work", "Hobbies", "Weather", "Love", "Sleep")
    var selected by remember { mutableStateOf(selectedReasons) }

    SheetLayout(
        title = "What's the reason?",
        onClose = onClose
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(reasons) { reason ->
                OutlinedButton(
                    onClick = {
                        selected =
                            if (selected.contains(reason)) selected - reason else selected + reason
                        onReasonsSelected(selected)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (selected.contains(reason)) Color.LightGray else Color.Transparent
                    )
                ) {
                    Text(text = reason, fontSize = 16.sp)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onNext, modifier = Modifier.align(Alignment.End)) {
            Text("Next")
        }
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
        title = "Write your journal",
        onClose = onClose
    ) {
        BasicTextField(
            value = journalEntry,
            onValueChange = onJournalEntryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
                .padding(8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 16.sp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onSave, modifier = Modifier.align(Alignment.End)) {
            Text("Save")
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
                    colors = listOf(Color.White, Color.LightGray)
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
                color = MaterialTheme.colorScheme.primary
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
    if (selectedEmotions.isEmpty()) return 50 // Default mood score

    val emotionScores = mapOf(
        "Happy" to 90,
        "Sad" to 25,
        "Excited" to 80,
        "Calm" to 70,
        "Confused" to 40,
        "Surprised" to 50
    )

    val totalScore = selectedEmotions.sumOf { emotionScores[it] ?: 50 }
    return totalScore / selectedEmotions.size
}
