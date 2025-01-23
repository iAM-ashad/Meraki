package com.iamashad.meraki.screens.chatbot

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.iamashad.meraki.R
import com.iamashad.meraki.components.ConnectivityObserver
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.navigation.Screens
import com.iamashad.meraki.utils.ConnectivityStatus
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens
import com.iamashad.meraki.utils.rememberWindowSizeClass

@Composable
fun ChatbotScreen(
    viewModel: ChatViewModel,
    navController: NavController,
) {
    val context = LocalContext.current
    val isConnected = ConnectivityStatus(context)
    val chatMessages = remember { viewModel.messageList }
    val isTyping by viewModel.isTyping
    val gradientColors = viewModel.determineGradientColors()

    val animatedColors = gradientColors.map { targetColor ->
        animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing)
        ).value
    }
    val animatedGradient = Brush.verticalGradient(colors = animatedColors)
    val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val dimens = LocalDimens.current

    var hasPreviousConversation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasPreviousConversation = viewModel.hasPreviousConversation()
        viewModel.initializeContext(userId)
    }
    val windowSize = rememberWindowSizeClass()

    ConnectivityObserver(connectivityStatus = isConnected) {
        ProvideDimens(windowSize) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedGradient)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (chatMessages.isEmpty()) {
                        NewConversationScreen(viewModel, hasPreviousConversation)
                    } else {
                        ChatHeader()

                        Spacer(modifier = Modifier.height(dimens.paddingMedium))

                        MessageList(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = dimens.paddingSmall),
                            messageList = chatMessages
                        )

                        if (isTyping) {
                            TypingIndicator()
                        }

                        ChatInputSection(
                            onMessageSend = { viewModel.sendMessage(it) },
                            onFinishConversation = {
                                viewModel.finishConversation(it)
                                navController.navigate(Screens.HOME.name)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueConversationButton(onClick: () -> Unit) {
    val dimens = LocalDimens.current
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(
            text = "Continue Last Conversation",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = dimens.fontSmall
            )
        )
    }
}


@Composable
fun ChatInputSection(onMessageSend: (String) -> Unit, onFinishConversation: (String) -> Unit) {
    val dimens = LocalDimens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FinishConversationButton(onFinish = onFinishConversation)
        MessageInput(onMessageSend = onMessageSend)
    }
}

@Composable
fun ChatHeader() {
    val lottieComposition =
        rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_chatheader))
    val lottieProgress = animateLottieCompositionAsState(
        composition = lottieComposition.value,
        iterations = LottieConstants.IterateForever
    ).progress
    val dimens = LocalDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = dimens.paddingMedium),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LottieAnimation(
            composition = lottieComposition.value,
            progress = { lottieProgress },
            modifier = Modifier.size((dimens.avatarSize / 2) * 1)
        )
        Spacer(modifier = Modifier.width(dimens.paddingSmall))
        Column {
            Text(
                text = "MERAKI ASSISTANT",
                style = MaterialTheme.typography.headlineMedium.copy(
                    letterSpacing = 2.sp,
                    fontSize = dimens.fontMedium,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.surface
            )
            Text(
                text = "Your AI mental health assistant",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = dimens.fontSmall),
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@Composable
fun NewConversationScreen(
    viewModel: ChatViewModel,
    hasPreviousConversation: Boolean
) {
    val dimens = LocalDimens.current
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary
        )
    )
    val windowSize = rememberWindowSizeClass()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp

    if (windowSize.widthSizeClass == WindowWidthSizeClass.Expanded ||
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                AnimatedAvatar(
                    modifier = Modifier.size(dimens.avatarSize)
                )
            }
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .clip(
                        RoundedCornerShape(
                            topStart = dimens.cornerRadius * 2,
                            bottomStart = dimens.cornerRadius * 2
                        )
                    )
                    .background(gradientBackground),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = dimens.paddingMedium)
                ) {
                    Text(
                        text = "Unable to escape your thoughts?",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier.padding(
                            top = dimens.paddingLarge * 2,
                            bottom = dimens.paddingMedium
                        )
                    )
                    Text(
                        text = "This is your safe space to express.",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        ),
                        modifier = Modifier.padding(bottom = dimens.paddingLarge)
                    )
                    Text(
                        text = "Tap below to start a conversation :)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        ),
                        modifier = Modifier.padding(bottom = dimens.paddingMedium)
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (hasPreviousConversation) {
                        ContinueConversationButton(
                            onClick = { viewModel.loadPreviousConversation() }
                        )
                    }

                    StartConversationButton(onClick = { viewModel.startNewConversation() })
                }

                Spacer(Modifier.padding(bottom = dimens.paddingLarge * 2))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Transparent)
                        .padding(dimens.paddingMedium)
                        .clip(RoundedCornerShape(dimens.cornerRadius / 2)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    ConfidentialityFooter()
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                AnimatedAvatar(
                    modifier = Modifier
                        .size((dimens.avatarSize / 10) * 8)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((screenHeightDp / 10) * 8)
                        .clip(
                            RoundedCornerShape(
                                topStart = dimens.cornerRadius * 2,
                                topEnd = dimens.cornerRadius * 2
                            )
                        )
                        .background(gradientBackground)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Unable to escape your thoughts?",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.padding(
                                top = dimens.paddingLarge * 2,
                                bottom = dimens.paddingMedium
                            )
                        )
                        Text(
                            text = "This is your safe space to express.",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            ),
                            modifier = Modifier.padding(bottom = dimens.paddingLarge)
                        )
                        Text(
                            text = "Tap below to start a conversation :)",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            ),
                            modifier = Modifier.padding(bottom = dimens.paddingMedium)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        if (hasPreviousConversation) {
                            ContinueConversationButton(
                                onClick = { viewModel.loadPreviousConversation() }
                            )
                        }

                        StartConversationButton(onClick = { viewModel.startNewConversation() })
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Transparent)
                            .padding(
                                start = dimens.paddingMedium,
                                end = dimens.paddingMedium,
                                top = dimens.paddingLarge
                            )
                            .clip(RoundedCornerShape(dimens.cornerRadius / 2)),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        ConfidentialityFooter()
                    }
                }
            }
        }
    }
}


@Composable
fun StartConversationButton(onClick: () -> Unit) {
    val lottieComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(
            R.raw.lottie_beating_heart
        )
    )
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition, iterations = LottieConstants.IterateForever
    )
    val dimens = LocalDimens.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(composition = lottieComposition,
            progress = { lottieProgress },
            modifier = Modifier
                .size((dimens.avatarSize))
                .clip(CircleShape)
                .clickable {
                    onClick.invoke()
                }
        )
    }
}


@Composable
fun AnimatedAvatar(modifier: Modifier = Modifier) {

    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_chatscreen))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition, iterations = LottieConstants.IterateForever
    )
    Box(
        modifier = modifier.background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = lottieComposition,
            progress = { lottieProgress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun ConfidentialityFooter(
    modifier: Modifier = Modifier
) {

    var showDialog by remember { mutableStateOf(false) }
    val dimens = LocalDimens.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimens.paddingLarge * 3)
            .padding(dimens.paddingMedium)
            .background(Color.Transparent)
            .clickable { showDialog = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_lock),
            contentDescription = "Confidentiality Lock",
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(dimens.avatarSize / 10)
        )
        Spacer(modifier = Modifier.width(dimens.paddingSmall))
        Text(
            text = "Your conversations are private",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.surface.copy(.7f)
            )
        )
    }

    if (showDialog) {
        AlertDialog(onDismissRequest = { showDialog = false }, title = {
            Text(
                text = "Privacy Assurance",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }, text = {
            Text(
                text = "All your conversations are stored securely on your device and are not uploaded to the cloud. You have complete control over your data.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            )
        }, confirmButton = {
            Button(
                onClick = { showDialog = false },
                shape = RoundedCornerShape(dimens.cornerRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    "Got it!"
                )
            }
        })
    }
}


@Composable
fun FinishConversationButton(onFinish: (String) -> Unit) {
    var showPopup by remember { mutableStateOf(false) }
    var conversationTag by remember { mutableStateOf("") }
    val dimens = LocalDimens.current
    if (showPopup) {
        AlertDialog(
            onDismissRequest = { showPopup = false },
            title = {
                Text(
                    "End Conversation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
                ) {
                    Text(
                        "Enter a tag for this conversation (e.g., Sleep Issues, Anxiety):",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    OutlinedTextField(
                        value = conversationTag,
                        onValueChange = { conversationTag = it },
                        placeholder = {
                            Text(
                                "What was this conversation about?",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(dimens.cornerRadius)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (conversationTag.isNotEmpty()) {
                            onFinish(conversationTag)
                            showPopup = false
                        }
                    },
                    elevation = ButtonDefaults.buttonElevation(dimens.elevation),
                    shape = RoundedCornerShape(dimens.cornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Text(
                        "Save"
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = { showPopup = false },
                    shape = RoundedCornerShape(dimens.cornerRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(dimens.elevation)
                ) {
                    Text(
                        "Cancel"
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
            textContentColor = MaterialTheme.colorScheme.onBackground
        )
    }

    Button(
        onClick = { showPopup = true },
        shape = RoundedCornerShape(dimens.cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = ButtonDefaults.buttonElevation(dimens.elevation)
    ) {
        Text(
            "Finish"
        )
    }
}

@Composable
fun TypingIndicator() {
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_typing_indicator))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition, iterations = LottieConstants.IterateForever
    )
    val dimens = LocalDimens.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimens.paddingSmall), contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = lottieComposition,
            progress = { lottieProgress },
            modifier = Modifier.size((dimens.avatarSize / 3) * 2)
        )
    }
}


@Composable
fun MessageList(modifier: Modifier = Modifier, messageList: List<Message>) {
    val dimens = LocalDimens.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(dimens.paddingSmall)
    ) {
        items(messageList.reversed()) { message ->
            MessageRow(message)
        }
    }
}


@Composable
fun MessageRow(message: Message) {
    val isModel = message.role == "model"
    val dimens = LocalDimens.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isModel) Arrangement.Start else Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .wrapContentWidth(unbounded = true)
                .widthIn(max = dimens.paddingLarge * 12)
                .clip(
                    if (isModel) RoundedCornerShape(
                        bottomStart = dimens.cornerRadius,
                        topEnd = dimens.cornerRadius,
                        topStart = dimens.paddingSmall / 2,
                        bottomEnd = dimens.paddingSmall / 2
                    )
                    else RoundedCornerShape(
                        bottomEnd = dimens.cornerRadius,
                        topStart = dimens.cornerRadius,
                        topEnd = dimens.paddingSmall / 2,
                        bottomStart = dimens.paddingSmall / 2
                    )
                )
                .background(
                    if (isModel) MaterialTheme.colorScheme.background
                    else MaterialTheme.colorScheme.inversePrimary
                )
                .padding(dimens.paddingMedium)
        ) {
            SelectionContainer {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Start
                    ),
                )
            }
        }
    }
}


@Composable
fun MessageInput(onMessageSend: (String) -> Unit) {
    var message by remember { mutableStateOf("") }
    val dimens = LocalDimens.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(dimens.paddingSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            placeholder = {
                Text(
                    "Type a message...",
                )
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = dimens.paddingLarge * 2, max = dimens.paddingLarge * 5),
            shape = RoundedCornerShape(dimens.cornerRadius),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = MaterialTheme.colorScheme.inversePrimary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface
            ),
            maxLines = 4
        )
        Spacer(modifier = Modifier.width(dimens.paddingSmall))
        IconButton(onClick = {
            if (message.isNotEmpty()) {
                onMessageSend(message)
                message = ""
            }
        }) {
            Icon(
                imageVector = Icons.AutoMirrored.Default.Send,
                contentDescription = "Send",
                tint = MaterialTheme.colorScheme.surface
            )
        }
    }
}



