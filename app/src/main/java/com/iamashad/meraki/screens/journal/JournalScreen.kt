package com.iamashad.meraki.screens.journal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iamashad.meraki.R
import com.iamashad.meraki.components.JournalCard
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens

@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onAddJournalClick: () -> Unit,
    onViewJournalClick: (Journal) -> Unit
) {
    val journals by viewModel.journals.collectAsState()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    ProvideDimens(screenWidth, screenHeight) {
        val dimens = LocalDimens.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

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

            FloatingActionButton(
                onClick = onAddJournalClick,
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(dimens.paddingMedium)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Add, contentDescription = "Add Journal"
                )
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
            contentDescription = "Make Journals",
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "• Reflect on your thoughts and emotions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "• Gain insights into your mental well-being.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Text(
                text = "• Create a safe space to express yourself.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(dimens.paddingLarge))

        Text(
            text = "Tap the plus icon below to write your first entry now!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}

