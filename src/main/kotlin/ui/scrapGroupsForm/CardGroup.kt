package ui.scrapGroupsForm

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import domain.models.TelegramGroup

@Composable
fun CardGroup(
    onDeleteClicked: (TelegramGroup) -> Unit,
    telegramGroup: TelegramGroup,
    modifier: Modifier = Modifier.padding(8.dp),
    elevation: Dp = 2.dp
) {
    val captionModifier = Modifier.padding(4.dp)

    Card(
        modifier = modifier,
        elevation = elevation
    ) {
        Row {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(0.8f)
            ) {
                Text("Name: ${telegramGroup.groupLink}", captionModifier)
                if (telegramGroup.chatId != null) {
                    Text("Name id: ${telegramGroup.chatId}", captionModifier)
                }
            }
            Box(
                modifier = Modifier.requiredSize(48.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { onDeleteClicked(telegramGroup) }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close, "Close"
                    )
                }
            }
        }
    }
}
