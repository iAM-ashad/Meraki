package com.iamashad.meraki.screens.journal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iamashad.meraki.R
import com.iamashad.meraki.components.JournalCard
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens

@OptIn(ExperimentalMaterial3Api::class)
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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    LaunchedEffect(errorState) {
        errorState?.let {
            snackBarHostState.showSnackbar(it)
        }
    }

    ProvideDimens(screenWidth, screenHeight) {
        val dimens = LocalDimens.current

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
                SearchBar(
                    query = searchQuery,
                    onQueryChanged = { viewModel.updateSearchQuery(it) },
                    onClearQuery = { viewModel.clearSearchResults() }
                )

                HeaderCard()

                if (journals.isEmpty()) {
                    EmptyJournalList()
                } else {
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

@Composable
fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit
) {
    val dimens = LocalDimens.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(dimens.cornerRadius / 2)
            )
            .padding(horizontal = dimens.paddingLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search Icon",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        TextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = { Text("Search...") },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .clip(RoundedCornerShape(dimens.cornerRadius)),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                unfocusedIndicatorColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                capitalization = KeyboardCapitalization.Words
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            ),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClearQuery) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )
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
            modifier = Modifier.padding(dimens.paddingMedium)
        ) {
            Column {
                Text(
                    text = "Capture Your Thoughts",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Write down your reflections and insights.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }
            Image(
                painter = painterResource(id = R.drawable.ic_journalscreen),
                contentDescription = null,
                modifier = Modifier.size(dimens.avatarSize / 5)
            )
        }
    }
}

@Composable
fun EmptyJournalList() {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimens.paddingMedium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_journal),
            contentDescription = "No Journals",
            modifier = Modifier.size((dimens.avatarSize / 3) * 2)
        )
        Spacer(modifier = Modifier.height(dimens.paddingSmall))

        Text(
            text = "Start Your Journaling Journey!",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimens.paddingLarge))

        Text(
            text = "Tap the plus icon below to write your first entry now!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}
