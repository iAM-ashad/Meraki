package com.iamashad.meraki.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import com.iamashad.meraki.model.Journal

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onAddJournalClick: () -> Unit,
    onViewJournal: (Journal) -> Unit
) {
    val pagedJournals = viewModel.pagedJournals.collectAsLazyPagingItems()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    val displayedJournals = if (isSearching) searchResults else pagedJournals.itemSnapshotList.items

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Header
        Text(
            text = "Your Journals",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Search Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = searchQuery,
                onValueChange = {
                    viewModel.updateSearchQuery(it)
                    if (it.isEmpty()) viewModel.clearSearchResults()
                    else viewModel.searchJournals(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                    .padding(16.dp),
                textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
        }

        // Journal List
        LazyColumn(
            modifier = Modifier.weight(1f), // Make the list scrollable and take available space
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (displayedJournals.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isSearching) "No results found." else "No journals found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else {
                items(displayedJournals) { journal ->
                    journal?.let {
                        JournalCard(
                            journal = it,
                            onDelete = { viewModel.deleteJournal(it.journalId) },
                            onClick = { onViewJournal(it) }
                        )
                    }
                }
                item {
                    if (pagedJournals.loadState.append == androidx.paging.LoadState.Loading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        // Add Journal Button
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddJournalClick,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(text = "Add Journal")
        }
    }
}




@Composable
fun JournalCard(journal: Journal, onDelete: () -> Unit, onClick: (Journal) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick(journal) }, // Navigate to ViewJournalScreen
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = journal.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = journal.content,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2 // Truncate long content
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Date: ${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", journal.date)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                Text(text = "Delete")
            }
        }
    }
}
