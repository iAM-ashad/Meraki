package com.iamashad.meraki.screens.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iamashad.meraki.model.Journal

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    userId: String,
    onAddJournalClick: () -> Unit
) {
    val journals = viewModel.journals.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text(
            text = "Your Journals",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn {
            items(journals.value) { journal ->
                JournalCard(journal, onDelete = { viewModel.deleteJournal(journal.journalId, userId) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onAddJournalClick, modifier = Modifier.align(Alignment.End)) {
            Text(text = "Add Journal")
        }
    }
}

@Composable
fun JournalCard(journal: Journal, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = journal.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = journal.content, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDelete) {
                Text(text = "Delete")
            }
        }
    }
}
