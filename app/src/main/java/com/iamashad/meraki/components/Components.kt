package com.iamashad.meraki.components

import android.content.Context
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.iamashad.meraki.R
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.utils.ConnectivityStatus
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.getMoodEmoji
import com.iamashad.meraki.utils.getMoodLabelFromTitle
import com.iamashad.meraki.utils.getTipForMood

fun showToast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
}

@Composable
fun AppSearchBar(
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
fun NoInternetScreen() {
    val dimens = LocalDimens.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimens.paddingMedium),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_nointernet),
            contentDescription = "No Internet",
            modifier = Modifier
                .size(dimens.avatarSize / 2)
                .padding(bottom = dimens.paddingMedium)
        )

        Text(
            text = "No Internet Connection",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.padding(bottom = dimens.paddingSmall)
        )

        Text(
            text = "It seems you are offline. Check your connection and try again to continue enjoying the app.",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = dimens.paddingLarge)
        )
    }
}

@Composable
fun ConnectivityObserver(
    connectivityStatus: ConnectivityStatus,
    content: @Composable () -> Unit
) {
    val isConnected by connectivityStatus.observeAsState(initial = true)

    if (!isConnected) {
        NoInternetScreen()
    } else {
        content()
    }
}

@Composable
fun EmotionChip(
    emotionName: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .size(dimens.paddingMedium * 5)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(dimens.paddingMedium * 4)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.Transparent,
                    shape = CircleShape
                ), contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = MaterialTheme.typography.headlineSmall.fontSize
            )
        }
        Text(
            text = emotionName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun ReasonChip(
    reason: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dimens = LocalDimens.current

    Surface(
        shape = RoundedCornerShape(dimens.cornerRadius),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .clickable(onClick = onClick)
    ) {
        Text(
            text = reason,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.background,
                textAlign = TextAlign.Center,
            ),
            modifier = Modifier.padding(
                horizontal = dimens.paddingSmall,
                vertical = dimens.paddingSmall
            )
        )
    }
}

@Composable
fun SheetLayout(
    title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit
) {
    val dimens = LocalDimens.current

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
            .padding(dimens.paddingMedium),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        Spacer(modifier = Modifier.height(dimens.paddingMedium))
        content()
    }
}

@Composable
fun JournalCard(
    journal: Journal, onEditClick: (Journal) -> Unit, onDeleteButtonClick: (String) -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dimens = LocalDimens.current

    Card(
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(dimens.elevation),
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
            Column(modifier = Modifier.padding(dimens.paddingMedium)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = getMoodEmoji(journal.title),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(end = dimens.paddingSmall)
                        )
                        Column {
                            Text(
                                text = getMoodLabelFromTitle(journal.title),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Text(
                                text = DateFormat.format("HH:mm", journal.date).toString(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
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

                Spacer(modifier = Modifier.height(dimens.paddingSmall))

                Text(
                    text = if (journal.title.isNotEmpty()) {
                        "You felt ${journal.title}"
                    } else {
                        "You felt Neutral"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )

                Text(
                    text = if (journal.reasons.isNotEmpty()) {
                        "Because of ${journal.reasons.joinToString()}"
                    } else {
                        "Because of unknown reasons"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                )

                Spacer(modifier = Modifier.height(dimens.paddingSmall))

                Text(
                    text = if (journal.content.isNotBlank()) {
                        "Note: ${journal.content}"
                    } else {
                        "No additional notes provided."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    maxLines = 1,
                    lineHeight = dimens.fontSmall
                )

                Spacer(modifier = Modifier.height(dimens.paddingSmall))

                if (journal.imageUrl != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_media),
                            contentDescription = "Media Attached",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(dimens.paddingMedium)
                        )
                        Spacer(modifier = Modifier.width(dimens.paddingSmall / 2))
                        Text(
                            text = "Media Attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.ic_no_media),
                            contentDescription = "No Media Attached",
                            tint = MaterialTheme.colorScheme.background,
                            modifier = Modifier.size(dimens.paddingMedium)
                        )
                        Spacer(modifier = Modifier.width(dimens.paddingSmall / 2))
                        Text(
                            text = "No Media Attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(dimens.paddingSmall))

                HorizontalDivider(color = MaterialTheme.colorScheme.surface, thickness = 0.5.dp)

                Spacer(modifier = Modifier.height(dimens.paddingSmall))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tip:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surface
                        )
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Icon",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .size(dimens.paddingMedium)
                                .clickable { showDeleteDialog = true }
                        )
                        Text(
                            text = "Delete",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.surface,
                                fontSize = MaterialTheme.typography.bodySmall.fontSize
                            ),
                            modifier = Modifier.padding(start = dimens.paddingSmall / 2)
                        )
                    }
                }
                Text(
                    text = getTipForMood(getMoodLabelFromTitle(journal.title)),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.surface,
                        fontSize = MaterialTheme.typography.bodySmall.fontSize * 0.8
                    )
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Journal") },
            text = { Text("Are you sure you want to delete this journal? This action cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteButtonClick(journal.journalId)
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            })
    }
}


@Composable
fun MoodTrendGraph(
    moodData: List<Pair<String, Int>>, modifier: Modifier = Modifier
) {
    if (moodData.isEmpty()) return

    val maxMood = moodData.maxOfOrNull { it.second } ?: 100
    val minMood = moodData.minOfOrNull { it.second } ?: 0
    val moodRange = maxMood - minMood
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            Color(220, 141, 243, 255), Color(53, 33, 59, 255)
        )
    )

    Canvas(
        modifier = modifier.fillMaxWidth()
    ) {
        val width = size.width
        val height = size.height
        val xStep = width / (moodData.size - 1).coerceAtLeast(1)
        val verticalPadding = 16.dp.toPx()
        val yStep = (height - 2 * verticalPadding) / moodRange.coerceAtLeast(1)

        val path = Path().apply {
            moodData.forEachIndexed { index, (_, mood) ->
                val x = index * xStep
                val y = height - verticalPadding - (mood - minMood) * yStep
                if (index == 0) moveTo(x, y)
                else cubicTo(
                    x - xStep / 2,
                    getY(moodData, index - 1, minMood, yStep, height, verticalPadding),
                    x - xStep / 2,
                    y,
                    x,
                    y
                )
            }
        }

        drawPath(
            path = path, brush = gradientBrush, style = Stroke(width = 6.dp.toPx())
        )
    }
}

private fun getY(
    moodData: List<Pair<String, Int>>,
    index: Int,
    minMood: Int,
    yStep: Float,
    height: Float,
    verticalPadding: Float
): Float {
    return height - verticalPadding - (moodData[index].second - minMood) * yStep
}