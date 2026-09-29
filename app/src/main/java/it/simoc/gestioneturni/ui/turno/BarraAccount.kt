package it.simoc.gestioneturni.ui.turno

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.simoc.gestioneturni.data.sync.StatoSync

/** Stato della sincronizzazione e menu account (invita, sincronizza, esci). */
@Composable
internal fun BarraAccount(
    statoSync: StatoSync,
    codiceInvito: String,
    email: String?,
    onSincronizza: () -> Unit,
    onEsci: () -> Unit,
) {
    var menuAperto by remember { mutableStateOf(false) }
    var invitoAperto by remember { mutableStateOf(false) }
    var confermaUscita by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = when {
                statoSync.inCorso -> "⟳ Sincronizzazione…"
                statoSync.modificheInAttesa > 0 ->
                    "● ${statoSync.modificheInAttesa} ${if (statoSync.modificheInAttesa == 1) "modifica" else "modifiche"} da inviare"
                else -> "✓ Sincronizzato"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (statoSync.modificheInAttesa > 0 && !statoSync.inCorso) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.weight(1f))
        Box {
            TextButton(onClick = { menuAperto = true }) { Text("Menu") }
            DropdownMenu(expanded = menuAperto, onDismissRequest = { menuAperto = false }) {
                if (email != null) {
                    Text(
                        email,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                DropdownMenuItem(
                    text = { Text("Invita l'altra persona") },
                    onClick = { menuAperto = false; invitoAperto = true },
                )
                DropdownMenuItem(
                    text = { Text("Sincronizza ora") },
                    onClick = { menuAperto = false; onSincronizza() },
                )
                DropdownMenuItem(
                    text = { Text("Esci") },
                    onClick = {
                        menuAperto = false
                        if (statoSync.modificheInAttesa > 0) confermaUscita = true else onEsci()
                    },
                )
            }
        }
    }

    if (invitoAperto) {
        InvitoDialog(codiceInvito, onChiudi = { invitoAperto = false })
    }

    if (confermaUscita) {
        AlertDialog(
            onDismissRequest = { confermaUscita = false },
            title = { Text("Modifiche non inviate") },
            text = {
                Text(
                    "Ci sono ${statoSync.modificheInAttesa} modifiche non ancora arrivate al server " +
                        "(serve internet). Se esci adesso andranno perse."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { confermaUscita = false; onEsci() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Esci comunque") }
            },
            dismissButton = {
                TextButton(onClick = { confermaUscita = false }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun InvitoDialog(codice: String, onChiudi: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onChiudi,
        title = { Text("Invita l'altra persona") },
        text = {
            Column {
                Text("Deve installare l'app, creare il suo account e inserire questo codice:")
                SelectionContainer {
                    Text(
                        codice,
                        style = MaterialTheme.typography.headlineMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Codice per il calendario turni in Gestione Turni: $codice")
                }
                context.startActivity(Intent.createChooser(intent, "Condividi il codice"))
            }) { Text("Condividi") }
        },
        dismissButton = {
            TextButton(onClick = onChiudi) { Text("Chiudi") }
        },
    )
}
