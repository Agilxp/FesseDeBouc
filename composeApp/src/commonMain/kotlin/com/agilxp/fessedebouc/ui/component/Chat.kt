package com.agilxp.fessedebouc.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agilxp.fessedebouc.getPlatform
import com.agilxp.fessedebouc.model.MessageDTO
import com.agilxp.fessedebouc.model.PostMessageDTO
import com.agilxp.fessedebouc.ui.viewmodel.GroupViewModel
import com.composables.core.Icon

@Composable
fun OutgoingMessage(text: String) {
    Box(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        contentAlignment = Alignment.TopEnd
    ) {
        val bubbleColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        Surface(shape = MaterialTheme.shapes.medium.copy(bottomEnd = CornerSize(4.dp)), color = bubbleColor) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = contentColorFor(backgroundColor = bubbleColor)
            )
        }
    }
}

@Composable
fun IncomingMessage(text: String) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(0.9f),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val bubbleColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
        Surface(shape = MaterialTheme.shapes.medium.copy(bottomStart = CornerSize(4.dp)), color = bubbleColor) {
            Text(text, modifier = Modifier.padding(12.dp), color = contentColorFor(backgroundColor = bubbleColor))
        }
    }
}

@Composable
fun ComposerBar(onSendClick: () -> Unit, text: String, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val isSendingAllowed = text.isNotBlank()

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextField(shape = RoundedCornerShape(100),
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (isSendingAllowed) {
                    onSendClick()
                }
            }),
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("Type your message here...") })
        IconButton(onClick = onSendClick, enabled = isSendingAllowed) {
            Icon(
                Icons.AutoMirrored.Rounded.Send, contentDescription = "Send", tint = if (isSendingAllowed) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.33f)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatWindow(
    groupViewModel: GroupViewModel
) {
    var composingText by remember { mutableStateOf("") }
    val groupUiState by groupViewModel.uiState.collectAsState()
    val state = rememberLazyListState()
    val userEmail = getPlatform().getUserEmail()
    LaunchedEffect(groupUiState.groupMessages) {
        state.animateScrollToItem(groupUiState.groupMessages.size)
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                LaunchedEffect(Unit) { // scroll to the most recent item when you start the screen
                    state.scrollToItem(Int.MAX_VALUE)
                }
                LazyColumn(
                    contentPadding = padding,
                    modifier = Modifier.widthIn(max = 800.dp).fillMaxWidth().weight(1f),
                    state = state,
                ) {
                    items(
                        count = groupUiState.groupMessages.size,
                        contentType = { MessageDTO },
                        itemContent = { index ->
                            val message = groupUiState.groupMessages[index]
                            when (message.sender.email) {
                                userEmail -> {
                                    OutgoingMessage(message.content!!)
                                }

                                else -> IncomingMessage(message.content!!)
                            }
                        })
                }
                ComposerBar(onSendClick = {
                    groupViewModel.sendMessage(PostMessageDTO(content = composingText))
                    composingText = ""
                }, text = composingText, onTextChange = {
                    composingText = it
                })
            }
        }
    }
}
