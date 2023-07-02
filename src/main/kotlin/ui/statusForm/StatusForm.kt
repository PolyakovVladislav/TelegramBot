package ui.statusForm

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatusForm(
    statusStore: StatusStore,
    modifier: Modifier = Modifier.padding(8.dp).requiredWidth(300.dp)
) {

    Surface(
        modifier = modifier
    ) {
        Column {
            Text(
                "Status:"
            )
            Text(
                statusStore.statusState?.title ?: ""
            )
            Text(
                statusStore.statusState?.status ?: ""
            )
        }
    }
}