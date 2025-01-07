package com.iamashad.meraki.screens.chatbot

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.iamashad.meraki.R
import com.iamashad.meraki.model.Message
import com.iamashad.meraki.navigation.Screens

@Composable
fun ChatbotScreen(
    viewModel: ChatViewModel,
    navController: NavController
) {
    val chatMessages = remember { viewModel.messageList }

    val gradientColors = viewModel.determineGradientColors()
    val animatedColors = gradientColors.map { targetColor ->
        animateColorAsState(
            targetValue = targetColor,
            animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing),
            label = ""
        ).value
    }
    val animatedGradient = Brush.verticalGradient(animatedColors)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedGradient)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            if (chatMessages.isEmpty()) {
                NewConversationScreen(viewModel)
            } else {

                ChatHeader()

                Spacer(modifier = Modifier.height(16.dp))

                MessageList(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    messageList = chatMessages
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FinishConversationButton(onFinish = { context ->
                        viewModel.finishConversation(context)
                        navController.navigate(Screens.HOME.name)
                    })
                    MessageInput(onMessageSend = { viewModel.sendMessage(it) })
                }
            }
        }
    }
}

@Composable
fun ChatHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .background(Color.Transparent)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Meraki Assistant",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.surface
                )
                Text(
                    text = "Your AI mental health assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Composable
fun NewConversationScreen(viewModel: ChatViewModel) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary
                    )
                )
            ), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            AnimatedAvatar(modifier = Modifier.size(200.dp))

            Spacer(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "Welcome to Meraki",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Text(
                text = "Your safe space to express and explore.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = { viewModel.startNewConversation() },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(
                    text = "Start New Conversation", fontSize = 18.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Transparent)
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            ConfidentialityFooter()
        }
    }
}

@Composable
fun AnimatedAvatar(modifier: Modifier = Modifier) {
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_chatbot))
    val lottieProgress by animateLottieCompositionAsState(
        composition = lottieComposition, iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Transparent),
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
fun ConfidentialityFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp)
            .padding(12.dp)
            .background(Color.Transparent),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_lock),
            contentDescription = "Confidentiality Lock",
            tint = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "Your conversations are private and secure.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.surface.copy(.7f),
        )
    }
}


@Composable
fun FinishConversationButton(onFinish: (String) -> Unit) {
    var showPopup by remember { mutableStateOf(false) }
    var conversationTag by remember { mutableStateOf("") }

    if (showPopup) {
        AlertDialog(onDismissRequest = { showPopup = false }, title = {
            Text(
                "End Conversation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }, text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Enter a tag for this conversation (e.g., Sleep Issues, Anxiety):")
                OutlinedTextField(value = conversationTag,
                    onValueChange = { conversationTag = it },
                    placeholder = { Text("What was this conversation about?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                )
            }
        }, confirmButton = {
            Button(
                onClick = {
                    if (conversationTag.isNotEmpty()) {
                        onFinish(conversationTag)
                        showPopup = false
                    }
                }, shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Text("Save")
            }
        }, dismissButton = {
            Button(
                onClick = { showPopup = false },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text("Cancel")
            }
        })
    }

    Button(
        onClick = { showPopup = true },
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text("Finish Conversation")
    }
}

@Composable
fun MessageList(modifier: Modifier = Modifier, messageList: List<Message>) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(messageList.reversed()) { message ->
            MessageRow(message)
        }
    }
}


@Composable
fun MessageRow(message: Message) {
    val isModel = message.role == "model"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isModel) Arrangement.Start else Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isModel) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.inversePrimary)
                .padding(12.dp)
        ) {
            SelectionContainer {
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun MessageInput(onMessageSend: (String) -> Unit) {
    var message by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(value = message,
            onValueChange = { message = it },
            placeholder = { Text("Type a message...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = MaterialTheme.colorScheme.inversePrimary,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
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


