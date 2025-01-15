package com.iamashad.meraki.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.utils.LoadImageWithGlide
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ViewJournalScreen(
    journal: Journal
) {
    var showFullSizeImage by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    ProvideDimens(screenWidth, screenHeight) {
        val dimens = LocalDimens.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = dimens.paddingMedium),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Card for Journal Content
                Card(
                    shape = RoundedCornerShape(dimens.cornerRadius),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimens.paddingMedium)
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall + (dimens.paddingSmall / 2))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = "You felt ${journal.title}",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold, fontSize = dimens.fontLarge
                                ),
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(dimens.paddingMedium)
                            )
                        }

                        // Show image if available
                        journal.imageUrl?.let { imageUrl ->
                            LoadImageWithGlide(
                                imageUrl = imageUrl,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .padding(dimens.paddingSmall + (dimens.paddingSmall / 2))
                                    .clickable {
                                        showFullSizeImage = true
                                    }
                            )
                        }

                        if (journal.reasons.isNotEmpty()) {
                            Text(
                                text = "Tags: ${journal.reasons.joinToString(", ")}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium, fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(
                                    start = dimens.paddingSmall + (dimens.paddingSmall / 2),
                                    top = dimens.paddingSmall
                                )
                            )
                        }

                        Text(
                            text = "Date: ${formatDate(journal.date)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = dimens.paddingSmall + (dimens.paddingSmall / 2)
                            )
                        )

                        Text(
                            text = journal.content,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .padding(dimens.paddingSmall + (dimens.paddingSmall / 2))
                                .verticalScroll(scrollState)
                        )
                    }
                }
            }
        }

        if (showFullSizeImage) {
            FullSizeImageDialog(
                imageUrl = journal.imageUrl!!,
                onDismiss = { showFullSizeImage = false }
            )
        }
    }
}

@Composable
fun FullSizeImageDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val dimens = LocalDimens.current

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            LoadImageWithGlide(
                imageUrl = imageUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(dimens.paddingMedium)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(dimens.paddingMedium)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Close Full-Size Image",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
