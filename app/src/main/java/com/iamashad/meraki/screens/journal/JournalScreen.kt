package com.iamashad.meraki.screens.journal

import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.iamashad.meraki.R
import com.iamashad.meraki.components.AppSearchBar
import com.iamashad.meraki.components.JournalCard
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.rememberWindowSizeClass

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onAddJournalClick: () -> Unit,
    onViewJournalClick: (Journal) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val journals by if (isSearching) {
        viewModel.searchResults.collectAsState()
    } else {
        viewModel.journals.collectAsState()
    }
    val errorState by viewModel.errorState.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorState) {
        errorState?.let {
            snackBarHostState.showSnackbar(it)
        }
    }
    val windowSize = rememberWindowSizeClass()
    ProvideDimens(windowSize) {
        val dimens = LocalDimens.current
        val isLargeScreen = windowSize.widthSizeClass == WindowWidthSizeClass.Expanded ||
                LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

        Scaffold(
            snackbarHost = { SnackbarHost(snackBarHostState) },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onAddJournalClick,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(dimens.paddingMedium)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Journal")
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AppSearchBar(
                    query = searchQuery,
                    onQueryChanged = { viewModel.updateSearchQuery(it) },
                    onClearQuery = { viewModel.clearSearchResults() }
                )

                HeaderCard()

                if (journals.isEmpty()) {
                    EmptyJournalList()
                } else {
                    if (isLargeScreen) {
                        // LazyVerticalGrid for larger screens
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                            horizontalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = dimens.paddingMedium)
                        ) {
                            items(journals) { journal ->
                                JournalCard(
                                    journal = journal,
                                    onEditClick = { onViewJournalClick(it) },
                                    onDeleteButtonClick = { viewModel.deleteJournal(journal.journalId) }
                                )
                            }
                        }
                    } else {
                        // LazyColumn for compact screens
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = dimens.paddingMedium)
                        ) {
                            items(journals) { journal ->
                                JournalCard(
                                    journal = journal,
                                    onEditClick = { onViewJournalClick(it) },
                                    onDeleteButtonClick = { viewModel.deleteJournal(journal.journalId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderCard() {
    val dimens = LocalDimens.current

    Surface(
        shape = RoundedCornerShape(dimens.cornerRadius),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .padding(dimens.paddingMedium)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.paddingMedium / 2)
        ) {
            Column {
                Text(
                    text = "Capture Your Thoughts",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
                Text(
                    text = "Write down your reflections and insights.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                )
            }
            Image(
                painter = painterResource(id = R.drawable.ic_journalscreen),
                contentDescription = null,
                modifier = Modifier.size(dimens.avatarSize / 3)
            )
        }
    }
}

@Composable
fun EmptyJournalList() {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_journal),
            contentDescription = "No Journals",
            modifier = Modifier.size((dimens.avatarSize))
        )

        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        Text(
            text = "Start Your Journaling Journey!",
            style = MaterialTheme.typography.titleLarge.copy(
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
        )

        Spacer(modifier = Modifier.height(dimens.paddingSmall / 2))

        Text(
            text = "Tap the plus icon below to write your first entry now!",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        )
    }
}
