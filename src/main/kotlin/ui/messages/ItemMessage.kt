package ui.messages

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import domain.models.Message

@Preview
@Composable
fun ItemMessage(
    message: Message,
    onDeleteClicked: (Message) -> Unit,
    modifier: Modifier = Modifier.padding(8.dp),
    elevation: Dp = 2.dp
) {
    Card(
        modifier = modifier,
        elevation = elevation
    ) {
            Column(Modifier.wrapContentWidth().padding(16.dp)) {
                Row {
                    Text(message.time)
                    IconButton(
                        onClick = { onDeleteClicked(message) }
                    ) {
                        Icon(Icons.Rounded.Close, "Close")
                    }
                }
                Text(message.message)
            }
    }
}
