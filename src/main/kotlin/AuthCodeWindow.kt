import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AuthCodeWindow(
    onAuthConfirmed: (String) -> Unit
) {

    val state = remember {
        mutableStateOf("")
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp).wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = state.value,
            onValueChange = { state.value = it },
            label = { Text("Enter confirmation code") }
        )
        Button(
            onClick = { onAuthConfirmed(state.value) }
        ) {
            Text("Confirm")
        }
    }
}