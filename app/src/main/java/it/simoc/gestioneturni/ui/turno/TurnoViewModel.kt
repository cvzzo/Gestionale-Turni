package it.simoc.gestioneturni.ui.turno

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.simoc.gestioneturni.data.TurnoRepository
import it.simoc.gestioneturni.data.local.database.Turno
import it.simoc.gestioneturni.data.sync.Sincronizzazione
import it.simoc.gestioneturni.data.sync.StatoSync
import it.simoc.gestioneturni.ui.turno.TurnoUiState.Error
import it.simoc.gestioneturni.ui.turno.TurnoUiState.Loading
import it.simoc.gestioneturni.ui.turno.TurnoUiState.Success
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TurnoViewModel @Inject constructor(
    private val turnoRepository: TurnoRepository,
    private val sincronizzazione: Sincronizzazione,
) : ViewModel() {

    val statoSync: StateFlow<StatoSync> = sincronizzazione.stato
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatoSync())

    init {
        // Quando l'altro telefono cambia un turno, si scaricano subito le novità.
        viewModelScope.launch {
            sincronizzazione.modificheRemote()
                .catch { Log.w("TurnoViewModel", "Aggiornamenti in tempo reale non disponibili", it) }
                .collect { sincronizzazione.richiedi() }
        }
    }

    private val giornoSelezionato = MutableStateFlow(LocalDate.now())

    private val eventiChannel = Channel<Evento>(Channel.BUFFERED)
    val eventi: Flow<Evento> = eventiChannel.receiveAsFlow()

    val uiState: StateFlow<TurnoUiState> = giornoSelezionato
        .map { YearMonth.from(it) }
        .distinctUntilChanged()
        .flatMapLatest { mese ->
            turnoRepository.turni(mese.atDay(1), mese.atEndOfMonth()).map { mese to it }
        }
        .combine(giornoSelezionato) { (mese, turni), giorno ->
            // Durante il cambio mese il giorno può essere già nel mese nuovo mentre i turni
            // sono ancora del vecchio: si scarta e si aspetta che arrivino quelli giusti.
            if (YearMonth.from(giorno) == mese) Success(mese, giorno, turni) else null
        }
        .filterNotNull()
        .map<Success, TurnoUiState> { it }
        .catch { emit(Error(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)

    fun selezionaGiorno(giorno: LocalDate) {
        giornoSelezionato.value = giorno
    }

    fun cambiaMese(delta: Long) {
        val mese = YearMonth.from(giornoSelezionato.value).plusMonths(delta)
        giornoSelezionato.value = if (mese == YearMonth.now()) LocalDate.now() else mese.atDay(1)
    }

    fun aggiungi(turno: Turno) {
        viewModelScope.launch {
            eventiChannel.send(Evento.Aggiunto(turnoRepository.salva(turno)))
        }
    }

    fun aggiorna(turno: Turno) {
        viewModelScope.launch { turnoRepository.salva(turno) }
    }

    fun elimina(turno: Turno, notifica: Boolean = true) {
        viewModelScope.launch {
            turnoRepository.elimina(turno)
            if (notifica) eventiChannel.send(Evento.Eliminato(turno))
        }
    }

    /** Annulla un'eliminazione: reinserisce il turno così com'era, con lo stesso id. */
    fun ripristina(turno: Turno) {
        viewModelScope.launch { turnoRepository.salva(turno) }
    }

    fun sincronizzaOra() {
        sincronizzazione.richiedi()
    }
}

sealed interface TurnoUiState {
    object Loading : TurnoUiState
    data class Error(val throwable: Throwable) : TurnoUiState
    data class Success(
        val mese: YearMonth,
        val giornoSelezionato: LocalDate,
        val turniDelMese: List<Turno>,
    ) : TurnoUiState {
        val turniDelGiorno: List<Turno> get() = turniDelMese.filter { it.data == giornoSelezionato }
    }
}

/** Eventi una tantum mostrati come snackbar con "Annulla". */
sealed interface Evento {
    data class Aggiunto(val turno: Turno) : Evento
    data class Eliminato(val turno: Turno) : Evento
}
