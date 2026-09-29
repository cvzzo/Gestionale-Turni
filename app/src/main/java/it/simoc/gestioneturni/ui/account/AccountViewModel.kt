package it.simoc.gestioneturni.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.simoc.gestioneturni.data.account.AccountRepository
import it.simoc.gestioneturni.data.account.StatoSessione
import it.simoc.gestioneturni.data.account.messaggioErrore
import it.simoc.gestioneturni.data.remoto.Famiglia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

sealed interface StatoApp {
    data object Caricamento : StatoApp
    data object NonConfigurato : StatoApp
    data object Accesso : StatoApp
    data object SceltaFamiglia : StatoApp
    data class Pronto(val famiglia: Famiglia, val email: String?) : StatoApp
}

data class OperazioneUi(
    val inCorso: Boolean = false,
    val errore: String? = null,
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val account: AccountRepository,
) : ViewModel() {

    val stato: StateFlow<StatoApp> = combine(account.sessione, account.famiglia) { sessione, famiglia ->
        when {
            !account.configurato -> StatoApp.NonConfigurato
            sessione is StatoSessione.Caricamento -> StatoApp.Caricamento
            sessione !is StatoSessione.Connesso -> StatoApp.Accesso
            famiglia == null -> StatoApp.SceltaFamiglia
            else -> StatoApp.Pronto(famiglia, sessione.email)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatoApp.Caricamento)

    private val _operazione = MutableStateFlow(OperazioneUi())
    val operazione: StateFlow<OperazioneUi> = _operazione

    fun accedi(email: String, password: String) = esegui {
        account.accedi(email, password)
        // Dopo un nuovo accesso la famiglia potrebbe esserci già sul server.
        account.aggiornaFamiglia()
    }

    fun registrati(email: String, password: String) = esegui {
        account.registrati(email, password)
        account.aggiornaFamiglia()
    }

    fun creaFamiglia() = esegui { account.creaFamiglia() }

    fun unisciti(codice: String) = esegui { account.unisciti(codice) }

    fun controllaFamiglia() = esegui { account.aggiornaFamiglia() }

    fun esci() = esegui { account.esci() }

    private fun esegui(azione: suspend () -> Unit) {
        viewModelScope.launch {
            _operazione.value = OperazioneUi(inCorso = true)
            try {
                azione()
                _operazione.value = OperazioneUi()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _operazione.value = OperazioneUi(errore = messaggioErrore(e))
            }
        }
    }
}
