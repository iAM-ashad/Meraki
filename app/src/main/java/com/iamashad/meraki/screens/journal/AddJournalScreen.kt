package com.iamashad.meraki.screens.journal

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import com.iamashad.meraki.components.EmotionChip
import com.iamashad.meraki.components.ReasonChip
import com.iamashad.meraki.components.SheetLayout
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.utils.LoadImageWithGlide
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.allEmotions
import com.iamashad.meraki.utils.allReasons
import com.iamashad.meraki.utils.calculateMoodScore
import com.iamashad.meraki.utils.commonlyUsedEmotions
import com.iamashad.meraki.utils.commonlyUsedReasons

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
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
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    ProvideDimens(screenWidth, screenHeight) {
        val dimens = LocalDimens.current
        BottomSheetScaffold(
            scaffoldState = bottomSheetState,
            sheetSwipeEnabled = false,
            sheetPeekHeight = dimens.paddingLarge * 4,
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
                        .padding(dimens.paddingMedium)
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            slideInHorizontally(
                                animationSpec = tween(500)
                            ) { it } togetherWith
                                    slideOutHorizontally(animationSpec = tween(500)) { -it }
                        }
                    ) { currentStep ->
                        when (currentStep) {
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
                                selectedImageUri = selectedImageUri,
                                onImageSelected = { selectedImageUri = it },
                                onSave = {
                                    viewModel.addJournal(
                                        Journal(
                                            journalId = journalId,
                                            userId = userId,
                                            title = selectedEmotions.joinToString(),
                                            content = journalEntry,
                                            moodScore = moodScore,
                                            reasons = selectedReasons,
                                            date = System.currentTimeMillis(),
                                            imageUrl = selectedImageUri?.toString()
                                        )
                                    )
                                    onSave()
                                    onClose()
                                },
                                onClose = onClose
                            )
                        }
                    }
                }
            },
            sheetShape = RoundedCornerShape(
                topStart = dimens.cornerRadius,
                topEnd = dimens.cornerRadius
            ),
            modifier = Modifier.fillMaxSize()
        ) {
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
                    ), contentAlignment = Alignment.Center
            ) {}
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
    var searchQuery by remember { mutableStateOf("") }
    val filteredEmotions = allEmotions.filter {
        it.first.contains(searchQuery, ignoreCase = true)
    }
    var selected by remember { mutableStateOf(selectedEmotions) }
    val dimens = LocalDimens.current

    SheetLayout(
        title = "Choose the emotions that match your mood",
        onClose = onClose,
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = dimens.paddingLarge * 3)
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
                        .padding(bottom = dimens.paddingMedium)
                )
                Text(
                    text = "Commonly Used",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = dimens.paddingSmall)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(dimens.paddingMedium)) {
                    items(commonlyUsedEmotions) { (emotionName, emoji) ->
                        EmotionChip(emotionName = emotionName,
                            emoji = emoji,
                            isSelected = selected.contains(emotionName),
                            onClick = {
                                selected =
                                    if (selected.contains(emotionName)) selected - emotionName else selected + emotionName
                                onEmotionsSelected(selected)
                            })
                    }
                }

                Spacer(modifier = Modifier.height(dimens.paddingMedium))

                Text(
                    text = "All Emotions",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = dimens.paddingSmall)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                    horizontalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(filteredEmotions) { (emotionName, emoji) ->
                        EmotionChip(emotionName = emotionName,
                            emoji = emoji,
                            isSelected = selected.contains(emotionName),
                            onClick = {
                                selected =
                                    if (selected.contains(emotionName)) selected - emotionName else selected + emotionName
                                onEmotionsSelected(selected)
                            })
                    }
                }
            }

            FloatingActionButton(
                onClick = onNext,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        top = dimens.paddingMedium,
                        bottom = dimens.paddingSmall / 2,
                        end = dimens.paddingSmall + (dimens.paddingSmall / 2),
                        start = dimens.paddingMedium
                    )
                    .clip(CircleShape)
            ) {
                Text(
                    "→", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
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
    var selected by remember { mutableStateOf(selectedReasons) }
    val dimens = LocalDimens.current

    SheetLayout(
        title = "What's the reason making you feel this way?", onClose = onClose
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = dimens.paddingLarge * 3)
            ) {
                Text(
                    text = "Commonly Used Reasons",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = dimens.paddingSmall)
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(dimens.paddingSmall)) {
                    items(commonlyUsedReasons) { reason ->
                        ReasonChip(reason = reason,
                            isSelected = selected.contains(reason),
                            onClick = {
                                selected =
                                    if (selected.contains(reason)) selected - reason else selected + reason
                                onReasonsSelected(selected)
                            })
                    }
                }

                Spacer(modifier = Modifier.height(dimens.paddingMedium))

                Text(
                    text = "All Reasons",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = dimens.paddingSmall)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                    horizontalArrangement = Arrangement.spacedBy(dimens.paddingMedium),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    items(allReasons) { reason ->
                        ReasonChip(reason = reason,
                            isSelected = selected.contains(reason),
                            onClick = {
                                selected =
                                    if (selected.contains(reason)) selected - reason else selected + reason
                                onReasonsSelected(selected)
                            })
                    }
                }
            }
            FloatingActionButton(
                onClick = onNext,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        top = dimens.paddingMedium,
                        bottom = dimens.paddingSmall / 2,
                        end = dimens.paddingSmall + (dimens.paddingSmall / 2),
                        start = dimens.paddingMedium
                    )
                    .clip(CircleShape)
            ) {
                Text(
                    "→", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun JournalEntrySheet(
    journalEntry: String,
    onJournalEntryChanged: (String) -> Unit,
    selectedImageUri: Uri?,
    onImageSelected: (Uri) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit
) {
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }
    val dimens = LocalDimens.current

    SheetLayout(
        title = "Add notes to your journal",
        onClose = onClose
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimens.paddingMedium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(dimens.paddingMedium)
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "Reflect on your emotions and add an image if you wish.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
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
                shape = RoundedCornerShape(dimens.cornerRadius),
                singleLine = false,
                maxLines = 10
            )
            Button(
                onClick = {
                    imagePickerLauncher.launch("image/*")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.paddingMedium),
                shape = RoundedCornerShape(dimens.cornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = selectedImageUri?.let { "Change Image" } ?: "Attach Image",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            selectedImageUri?.let { uri ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(dimens.paddingLarge * 8)
                        .clip(RoundedCornerShape(dimens.cornerRadius))
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    LoadImageWithGlide(
                        imageUrl = uri.toString(),
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Button(
                onClick = onSave,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = dimens.paddingMedium)
                    .fillMaxWidth(.6f),
                shape = RoundedCornerShape(dimens.cornerRadius),
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

