package it.simoc.gestioneturni.ui.turno

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.simoc.gestioneturni.data.TipoTurno
import it.simoc.gestioneturni.data.durataMinuti
import it.simoc.gestioneturni.data.formattaDurata
import it.simoc.gestioneturni.data.formattaOra
import it.simoc.gestioneturni.data.local.database.Turno
import it.simoc.gestioneturni.data.straordinarioMinuti

private enum class Campo { INIZIO, FINE }

/**
 * Modifica di un singolo turno: cambia solo questo turno, mai gli orari standard del tipo.
 * [onElimina] è null per un turno non ancora salvato.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModificaTurnoDialog(
    turnoIniziale: Turno,
    onConferma: (Turno) -> Unit,
    onElimina: (() -> Unit)?,
    onAnnulla: () -> Unit,
) {
    var turno by remember(turnoIniziale) { mutableStateOf(turnoIniziale) }
    var campoInModifica by remember { mutableStateOf<Campo?>(null) }

    AlertDialog(
        onDismissRequest = onAnnulla,
        title = { Text(if (turno.nuovo) "Nuovo turno" else "Modifica turno") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(formattaGiorno(turno.data), style = MaterialTheme.typography.titleSmall)

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoTurno.entries.forEach { tipo ->
                        FilterChip(
                            selected = turno.tipo == tipo,
                            onClick = {
                                // Lo straordinario tiene gli orari scelti, se ce ne sono.
                                turno = if (tipo == TipoTurno.STRAORDINARIO && !turno.tipo.giornaliero) {
                                    turno.copy(tipo = tipo)
                                } else {
                                    turno.copy(tipo = tipo, inizio = tipo.inizioStandard, fine = tipo.fineStandard)
                                }
                            },
                            label = { Text(tipo.etichetta) },
                            leadingIcon = { BollinoTurno(tipo, dimensione = 18.dp) },
                        )
                    }
                }

                if (turno.tipo.giornaliero) {
                    Text(
                        text = "Tutto il giorno, non conta nelle ore lavorate.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OrariTurno(turno, onModifica = { turno = it }, onApriSelettore = { campoInModifica = it })
                }

                OutlinedTextField(
                    value = turno.note,
                    onValueChange = { turno = turno.copy(note = it) },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConferma(turno) }) { Text("Salva") }
        },
        dismissButton = {
            Row {
                if (onElimina != null) {
                    TextButton(
                        onClick = onElimina,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Elimina") }
                }
                TextButton(onClick = onAnnulla) { Text("Annulla") }
            }
        },
    )

    campoInModifica?.let { campo ->
        SelettoreOrarioDialog(
            minuti = if (campo == Campo.INIZIO) turno.inizio else turno.fine,
            onConferma = { minuti ->
                turno = if (campo == Campo.INIZIO) turno.copy(inizio = minuti) else turno.copy(fine = minuti)
                campoInModifica = null
            },
            onAnnulla = { campoInModifica = null },
        )
    }
}

@Composable
private fun OrariTurno(
    turno: Turno,
    onModifica: (Turno) -> Unit,
    onApriSelettore: (Campo) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RigaOrario(
            etichetta = "Inizio",
            minuti = turno.inizio,
            scorciatoie = listOf(-60, -30, -15, 15),
            onClick = { onApriSelettore(Campo.INIZIO) },
            onSposta = { delta -> onModifica(turno.copy(inizio = (turno.inizio + delta).mod(24 * 60))) },
        )
        RigaOrario(
            etichetta = "Fine",
            minuti = turno.fine,
            scorciatoie = listOf(-15, 15, 30, 60, 120),
            onClick = { onApriSelettore(Campo.FINE) },
            onSposta = { delta -> onModifica(turno.copy(fine = (turno.fine + delta).mod(24 * 60))) },
        )

        Text(
            text = buildString {
                append("Durata ${formattaDurata(turno.durataMinuti)}")
                if (turno.straordinarioMinuti > 0) {
                    append(" · straordinario ${formattaDurata(turno.straordinarioMinuti)}")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RigaOrario(
    etichetta: String,
    minuti: Int,
    scorciatoie: List<Int>,
    onClick: () -> Unit,
    onSposta: (Int) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(etichetta, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = onClick) {
                Text(formattaOra(minuti), style = MaterialTheme.typography.titleMedium)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            scorciatoie.forEach { delta ->
                SuggestionChip(onClick = { onSposta(delta) }, label = { Text(etichettaDelta(delta)) })
            }
        }
    }
}

private fun etichettaDelta(delta: Int): String {
    val segno = if (delta < 0) "−" else "+"
    val assoluto = kotlin.math.abs(delta)
    return if (assoluto % 60 == 0) "$segno${assoluto / 60}h" else "$segno${assoluto}m"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelettoreOrarioDialog(
    minuti: Int,
    onConferma: (Int) -> Unit,
    onAnnulla: () -> Unit,
) {
    // TimeInput (tastiera) invece dell'orologio: con orari come 07:48 è molto più rapido.
    val stato = rememberTimePickerState(initialHour = minuti / 60, initialMinute = minuti % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onAnnulla,
        title = { Text("Scegli l'orario") },
        text = { TimeInput(state = stato) },
        confirmButton = {
            TextButton(onClick = { onConferma(stato.hour * 60 + stato.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onAnnulla) { Text("Annulla") }
        },
    )
}
