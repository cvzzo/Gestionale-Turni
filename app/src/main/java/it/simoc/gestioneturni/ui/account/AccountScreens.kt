package it.simoc.gestioneturni.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.simoc.gestioneturni.ui.theme.GestioneTurniTheme

@Composable
fun AccessoScreen(
    operazione: OperazioneUi,
    onAccedi: (email: String, password: String) -> Unit,
    onRegistrati: (email: String, password: String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    // Pulsanti sempre attivi: se manca qualcosa si spiega cosa, invece di lasciarli grigi.
    var avviso by rememberSaveable { mutableStateOf<String?>(null) }
    val controllaEPoi = { azione: (String, String) -> Unit ->
        avviso = when {
            !email.contains('@') -> "Scrivi la tua email."
            password.length < 6 -> "La password deve avere almeno 6 caratteri."
            else -> null
        }
        if (avviso == null) azione(email, password)
    }

    SchermataCentrata {
        Text("Gestione Turni", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Accedi per vedere e modificare i turni insieme, da due telefoni.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; avviso = null },
            label = { Text("Email") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; avviso = null },
            label = { Text("Password (almeno 6 caratteri)") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        avviso?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        EsitoOperazione(operazione)
        Button(
            onClick = { controllaEPoi(onAccedi) },
            enabled = !operazione.inCorso,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Accedi") }
        OutlinedButton(
            onClick = { controllaEPoi(onRegistrati) },
            enabled = !operazione.inCorso,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Prima volta? Crea un account") }
    }
}

@Composable
fun FamigliaScreen(
    operazione: OperazioneUi,
    onCrea: () -> Unit,
    onUnisciti: (codice: String) -> Unit,
    onControlla: () -> Unit,
    onEsci: () -> Unit,
) {
    var codice by rememberSaveable { mutableStateOf("") }
    // Se l'utente fa già parte di una famiglia (es. reinstallazione) si entra direttamente.
    LaunchedEffect(Unit) { onControlla() }

    SchermataCentrata {
        Text("Calendario condiviso", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Chi usa l'app per primo crea il calendario e riceve un codice. L'altra persona inserisce quel codice per vedere e modificare gli stessi turni.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onCrea, enabled = !operazione.inCorso, modifier = Modifier.fillMaxWidth()) {
            Text("Crea un nuovo calendario")
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        OutlinedTextField(
            value = codice,
            onValueChange = { codice = it.uppercase().take(8) },
            label = { Text("Codice ricevuto") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { onUnisciti(codice) },
            enabled = codice.length == 8 && !operazione.inCorso,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Unisciti con il codice") }
        EsitoOperazione(operazione)
        TextButton(onClick = onEsci, enabled = !operazione.inCorso) { Text("Esci dall'account") }
    }
}

@Composable
fun NonConfiguratoScreen() {
    SchermataCentrata {
        Text("Supabase non configurato", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Aggiungi SUPABASE_URL e SUPABASE_ANON_KEY in local.properties e ricompila l'app.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun CaricamentoScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun EsitoOperazione(operazione: OperazioneUi) {
    when {
        operazione.inCorso -> CircularProgressIndicator()
        operazione.errore != null -> Text(
            operazione.errore,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SchermataCentrata(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun AccessoPreview() {
    GestioneTurniTheme {
        AccessoScreen(OperazioneUi(errore = "Email o password non corretti."), onAccedi = { _, _ -> }, onRegistrati = { _, _ -> })
    }
}

@Preview(showBackground = true)
@Composable
private fun FamigliaPreview() {
    GestioneTurniTheme {
        FamigliaScreen(OperazioneUi(), onCrea = {}, onUnisciti = {}, onControlla = {}, onEsci = {})
    }
}
