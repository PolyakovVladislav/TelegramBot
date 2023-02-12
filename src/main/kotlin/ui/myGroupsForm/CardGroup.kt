package ui.myGroupsForm

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import domain.models.TelegramGroup

@Preview
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
            Column(Modifier.wrapContentWidth().padding(16.dp)) {
                Text("Name: ${telegramGroup.groupLink}", captionModifier)
                if (telegramGroup.chatId != null) {
                    Text("Group id: ${telegramGroup.chatId}", captionModifier)
                }
            }
            IconButton(
                onClick = { onDeleteClicked(telegramGroup) }
            ) {
                Icon(Icons.Rounded.Close, "Close")
            }
        }
    }
}
