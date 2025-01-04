package com.iamashad.meraki.screens.journal

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamashad.meraki.model.Journal

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onAddJournalClick: () -> Unit,
    onEditJournalClick: (Journal) -> Unit
) {
    val journals by viewModel.journals.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        Text(
            text = "Your Journals",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.Start)
        )

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(journals) { journal ->
                    JournalCard(
                        journal = journal,
                        onEditClick = { onEditJournalClick(it) },
                        onDeleteButtonClick = { viewModel.deleteJournal(journal.journalId) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddJournalClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(text = "Add Journal")
        }
    }
}


@Composable
fun JournalCard(
    journal: Journal,
    onEditClick: (Journal) -> Unit,
    onDeleteButtonClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = getMoodEmoji(journal.title),
                            fontSize = 24.sp,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = getMoodLabelFromTitle(journal.title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = DateFormat.format("HH:mm", journal.date)
                                    .toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    IconButton(onClick = { onEditClick(journal) }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "View Journal",
                            tint = MaterialTheme.colorScheme.surface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You felt ${journal.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = if (journal.reasons.isNotEmpty()) {
                        "Because of ${journal.reasons.joinToString()}"
                    } else {
                        "Because of Unknown Reasons"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fixed note display
                Text(
                    text = if (journal.content.isNotBlank()) {
                        "Note: ${journal.content}"
                    } else {
                        "No additional notes provided."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Divider(color = MaterialTheme.colorScheme.surface, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Find peace",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.surface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Icon",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    onDeleteButtonClick(journal.journalId)
                                }
                        )
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                Text(
                    text = "Spend time outdoors, surrounded by greenery and fresh air",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}


fun getMoodEmoji(title: String): String {
    return when {
        title.contains("Happy", true) -> "😊"
        title.contains("Sad", true) -> "😢"
        title.contains("Excited", true) -> "🤩"
        title.contains("Calm", true) -> "😌"
        title.contains("Confused", true) -> "😕"
        title.contains("Surprised", true) -> "😲"
        else -> "😶"
    }
}

fun getMoodLabelFromTitle(title: String): String {
    return when {
        title.contains("Happy", true) -> "Good"
        title.contains("Sad", true) -> "Bad"
        title.contains("Excited", true) -> "Excited"
        title.contains("Calm", true) -> "Calm"
        title.contains("Confused", true) -> "Confused"
        title.contains("Surprised", true) -> "Surprised"
        else -> "Unknown"
    }
}
