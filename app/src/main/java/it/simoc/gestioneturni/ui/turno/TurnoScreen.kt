package it.simoc.gestioneturni.ui.turno

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.simoc.gestioneturni.data.TipoTurno
import it.simoc.gestioneturni.data.di.fakeTurni
import it.simoc.gestioneturni.data.durataMinuti
import it.simoc.gestioneturni.data.formattaDurata
import it.simoc.gestioneturni.data.formattaOra
import it.simoc.gestioneturni.data.local.database.Turno
import it.simoc.gestioneturni.data.nuovoTurno
import it.simoc.gestioneturni.data.riepilogo
import it.simoc.gestioneturni.data.straordinarioMinuti
import it.simoc.gestioneturni.data.sync.StatoSync
import it.simoc.gestioneturni.ui.theme.GestioneTurniTheme
import it.simoc.gestioneturni.ui.theme.colori
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val ITALIANO = Locale.ITALIAN

@Composable
fun TurnoScreen(
    codiceInvito: String,
    email: String?,
    onEsci: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TurnoViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val statoSync by viewModel.statoSync.collectAsStateWithLifecycle()

    // Ogni volta che si torna nell'app si scaricano le modifiche dell'altro telefono.
    LifecycleResumeEffect(viewModel) {
        viewModel.sincronizzaOra()
        onPauseOrDispose {}
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventi.collect { evento ->
            launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val (messaggio, annulla) = when (evento) {
                    is Evento.Aggiunto -> "${evento.turno.tipo.etichetta} aggiunto" to { viewModel.elimina(evento.turno, notifica = false) }
                    is Evento.Eliminato -> "${evento.turno.tipo.etichetta} eliminato" to { viewModel.ripristina(evento.turno) }
                }
                val risultato = snackbarHostState.showSnackbar(messaggio, "Annulla", duration = SnackbarDuration.Short)
                if (risultato == SnackbarResult.ActionPerformed) annulla()
            }
        }
    }

    when (val s = state) {
        is TurnoUiState.Success -> TurnoScreen(
            state = s,
            snackbarHostState = snackbarHostState,
            onGiornoClick = viewModel::selezionaGiorno,
            onCambiaMese = viewModel::cambiaMese,
            onAggiungi = viewModel::aggiungi,
            onAggiorna = viewModel::aggiorna,
            onElimina = { viewModel.elimina(it) },
            statoSync = statoSync,
            codiceInvito = codiceInvito,
            email = email,
            onSincronizza = viewModel::sincronizzaOra,
            onEsci = onEsci,
            modifier = modifier,
        )
        is TurnoUiState.Error -> Text("Errore: ${s.throwable.message}", modifier.padding(16.dp))
        TurnoUiState.Loading -> Unit
    }
}

@Composable
internal fun TurnoScreen(
    state: TurnoUiState.Success,
    snackbarHostState: SnackbarHostState,
    onGiornoClick: (LocalDate) -> Unit,
    onCambiaMese: (Long) -> Unit,
    onAggiungi: (Turno) -> Unit,
    onAggiorna: (Turno) -> Unit,
    onElimina: (Turno) -> Unit,
    modifier: Modifier = Modifier,
    statoSync: StatoSync = StatoSync(),
    codiceInvito: String = "",
    email: String? = null,
    onSincronizza: () -> Unit = {},
    onEsci: () -> Unit = {},
) {
    // Il turno aperto nel dialog: id vuoto = nuovo, altrimenti esistente.
    var inModifica by remember { mutableStateOf<Turno?>(null) }
    val bozza = { tipo: TipoTurno -> nuovoTurno(tipo, state.giornoSelezionato, state.turniDelGiorno) }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            PulsantiRapidi(
                attivi = state.turniDelGiorno.map { it.tipo }.toSet(),
                onClick = { tipo ->
                    val esistente = state.turniDelGiorno.firstOrNull { it.tipo == tipo }
                    when {
                        // Lo straordinario non ha orari fissi e può essercene più d'uno: sempre il dialog.
                        tipo == TipoTurno.STRAORDINARIO -> inModifica = bozza(tipo)
                        // Gli altri pulsanti fanno da interruttore: se il turno c'è già, lo tolgono.
                        esistente != null -> onElimina(esistente)
                        else -> onAggiungi(bozza(tipo))
                    }
                },
                onLongClick = { tipo -> inModifica = bozza(tipo) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BarraAccount(
                statoSync = statoSync,
                codiceInvito = codiceInvito,
                email = email,
                onSincronizza = onSincronizza,
                onEsci = onEsci,
            )
            IntestazioneMese(
                mese = state.mese,
                onPrecedente = { onCambiaMese(-1) },
                onSuccessivo = { onCambiaMese(1) },
                onOggi = { onGiornoClick(LocalDate.now()) },
            )
            Calendario(
                mese = state.mese,
                giornoSelezionato = state.giornoSelezionato,
                turni = state.turniDelMese,
                onGiornoClick = onGiornoClick,
            )
            RiepilogoMese(state.turniDelMese)
            DettaglioGiorno(
                giorno = state.giornoSelezionato,
                turni = state.turniDelGiorno,
                onTurnoClick = { inModifica = it },
            )
        }
    }

    inModifica?.let { turno ->
        ModificaTurnoDialog(
            turnoIniziale = turno,
            onConferma = {
                if (it.nuovo) onAggiungi(it) else onAggiorna(it)
                inModifica = null
            },
            onElimina = if (turno.nuovo) null else {
                {
                    onElimina(turno)
                    inModifica = null
                }
            },
            onAnnulla = { inModifica = null },
        )
    }
}

@Composable
private fun IntestazioneMese(
    mese: YearMonth,
    onPrecedente: () -> Unit,
    onSuccessivo: () -> Unit,
    onOggi: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onPrecedente) { Text("◀", style = MaterialTheme.typography.titleMedium) }
        Text(
            text = "${mese.month.getDisplayName(TextStyle.FULL_STANDALONE, ITALIANO).maiuscola()} ${mese.year}",
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
        )
        TextButton(onClick = onSuccessivo) { Text("▶", style = MaterialTheme.typography.titleMedium) }
    }
    if (mese != YearMonth.now()) {
        TextButton(onClick = onOggi, modifier = Modifier.fillMaxWidth()) { Text("Torna a oggi") }
    }
}

@Composable
private fun RiepilogoMese(turni: List<Turno>) {
    val riepilogo = turni.riepilogo()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp)) {
            Statistica("Turni", riepilogo.numeroTurni.toString(), Modifier.weight(1f))
            Statistica("Ore totali", formattaDurata(riepilogo.minutiTotali), Modifier.weight(1f))
            Statistica("Straordinari", formattaDurata(riepilogo.minutiStraordinario), Modifier.weight(1f))
        }
        Text(
            text = "Riposi: ${riepilogo.giorniRiposo} · Ferie: ${riepilogo.giorniFerie}",
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Statistica(etichetta: String, valore: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valore, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(etichetta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DettaglioGiorno(
    giorno: LocalDate,
    turni: List<Turno>,
    onTurnoClick: (Turno) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formattaGiorno(giorno), style = MaterialTheme.typography.titleMedium)
        if (turni.isEmpty()) {
            Text(
                text = "Nessun turno. Tocca un pulsante qui sotto per aggiungerlo (toccalo di nuovo per toglierlo), tienilo premuto per cambiare l'orario prima.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        turni.forEach { turno ->
            Card(onClick = { onTurnoClick(turno) }, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BollinoTurno(turno.tipo, dimensione = 36.dp)
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = if (turno.tipo.giornaliero) {
                                turno.tipo.etichetta
                            } else {
                                "${turno.tipo.etichetta}  ${formattaOra(turno.inizio)} – ${formattaOra(turno.fine)}"
                            },
                            style = MaterialTheme.typography.titleSmall,
                        )
                        if (turno.tipo.giornaliero) Text(
                            text = "Tutto il giorno",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ) else Text(
                            text = buildString {
                                append(formattaDurata(turno.durataMinuti))
                                if (turno.tipo != TipoTurno.STRAORDINARIO && turno.straordinarioMinuti > 0) {
                                    append(" · +${formattaDurata(turno.straordinarioMinuti)} straordinario")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (turno.straordinarioMinuti > 0) {
                                TipoTurno.STRAORDINARIO.colori.sfondo
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        if (turno.note.isNotBlank()) {
                            Text(turno.note, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("Modifica", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PulsantiRapidi(
    attivi: Set<TipoTurno>,
    onClick: (TipoTurno) -> Unit,
    onLongClick: (TipoTurno) -> Unit,
) {
    Surface(tonalElevation = 3.dp, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TipoTurno.MATTINA, TipoTurno.POMERIGGIO, TipoTurno.NOTTE).forEach { tipo ->
                    PulsanteTurno(
                        titolo = tipo.etichetta,
                        sottotitolo = "${formattaOra(tipo.inizioStandard)}–${formattaOra(tipo.fineStandard)}",
                        tipo = tipo,
                        attivo = tipo in attivi,
                        onClick = { onClick(tipo) },
                        onLongClick = { onLongClick(tipo) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PulsanteTurno(
                    titolo = "+ Straordinario",
                    sottotitolo = null,
                    tipo = TipoTurno.STRAORDINARIO,
                    onClick = { onClick(TipoTurno.STRAORDINARIO) },
                    onLongClick = { onLongClick(TipoTurno.STRAORDINARIO) },
                    modifier = Modifier.weight(1f),
                )
                listOf(TipoTurno.RIPOSO, TipoTurno.FERIE).forEach { tipo ->
                    PulsanteTurno(
                        titolo = tipo.etichetta,
                        sottotitolo = null,
                        tipo = tipo,
                        attivo = tipo in attivi,
                        onClick = { onClick(tipo) },
                        onLongClick = { onLongClick(tipo) },
                        modifier = Modifier.weight(0.5f),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PulsanteTurno(
    titolo: String,
    sottotitolo: String?,
    tipo: TipoTurno,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    attivo: Boolean = false,
) {
    val colori = tipo.colori
    val forma = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(forma)
            .background(colori.sfondo)
            // Già presente nel giorno: il tap lo toglie, quindi si evidenzia.
            .then(if (attivo) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, forma) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (attivo) "✓ $titolo" else titolo, color = colori.testo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        if (sottotitolo != null) {
            Text(sottotitolo, color = colori.testo, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private val FORMATO_GIORNO = DateTimeFormatter.ofPattern("EEEE d MMMM", ITALIANO)

internal fun formattaGiorno(giorno: LocalDate): String = giorno.format(FORMATO_GIORNO).maiuscola()

private fun String.maiuscola() = replaceFirstChar { it.titlecase(ITALIANO) }

// Previews

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DefaultPreview() {
    GestioneTurniTheme {
        val oggi = LocalDate.now()
        TurnoScreen(
            state = TurnoUiState.Success(YearMonth.from(oggi), oggi, fakeTurni),
            snackbarHostState = remember { SnackbarHostState() },
            onGiornoClick = {}, onCambiaMese = {}, onAggiungi = {}, onAggiorna = {}, onElimina = {},
        )
    }
}
