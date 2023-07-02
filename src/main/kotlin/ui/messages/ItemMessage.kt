package ui.messages

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.VerticalAlignmentLine
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import domain.models.Message

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
            Column(
                Modifier.padding(start = 16.dp, bottom = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(message.time)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(
                            modifier = Modifier.requiredSize(48.dp),
                            onClick = { onDeleteClicked(message) }
                        ) {
                            Icon(Icons.Rounded.Close, "Close")
                        }
                    }
                }
                Text(message.message)
            }
    }
}
