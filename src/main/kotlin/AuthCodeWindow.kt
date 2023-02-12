import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
@Preview
fun AuthCodeWindow(
    onAuthConfirmed: (String) -> Unit
) {

    val state = remember {
        mutableStateOf("")
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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