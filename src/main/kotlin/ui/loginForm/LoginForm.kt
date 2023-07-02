package ui.loginForm

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import configurationRepository

@Composable
fun LoginForm(
    modifier: Modifier = Modifier.padding(8.dp).requiredWidth(300.dp)
) {
    val loginDataStore = remember {
        LoginStore(configurationRepository)
    }

    Surface(
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Column {
            OutlinedTextField(
                value = loginDataStore.state.phoneNumber ?: "",
                onValueChange = { loginDataStore.editPhoneNumber(it) },
                label = { Text("Phone number") }
            )
            OutlinedTextField(
                value = loginDataStore.state.apiId ?: "",
                onValueChange = { loginDataStore.editApiId(it) },
                label = { Text("API Id") }
            )
            OutlinedTextField(
                value = loginDataStore.state.apiHash ?: "",
                onValueChange = { loginDataStore.editApiHash(it) },
                label = { Text("API Hash") }
            )
        }
    }
}
