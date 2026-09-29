package it.simoc.gestioneturni.data.account

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import it.simoc.gestioneturni.BuildConfig
import it.simoc.gestioneturni.data.local.database.TurnoDao
import it.simoc.gestioneturni.data.remoto.Famiglia
import it.simoc.gestioneturni.data.sync.PreferenzeSync
import it.simoc.gestioneturni.data.sync.Sincronizzazione
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

sealed interface StatoSessione {
    data object Caricamento : StatoSessione
    data object NonConnesso : StatoSessione
    data class Connesso(val email: String?) : StatoSessione
}

@Singleton
class AccountRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val turnoDao: TurnoDao,
    private val preferenze: PreferenzeSync,
    private val sincronizzazione: Sincronizzazione,
) {
    val configurato: Boolean =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    val sessione: Flow<StatoSessione> = supabase.auth.sessionStatus.map { stato ->
        when (stato) {
            is SessionStatus.Authenticated -> StatoSessione.Connesso(stato.session.user?.email)
            // Sessione salvata ma rinnovo fallito (di solito: niente rete). Si resta dentro e si
            // lavora offline; supabase-kt riprova da solo.
            is SessionStatus.RefreshFailure -> StatoSessione.Connesso(null)
            is SessionStatus.NotAuthenticated -> StatoSessione.NonConnesso
            SessionStatus.Initializing -> StatoSessione.Caricamento
        }
    }

    /** La famiglia salvata sul telefono: c'è anche offline. */
    val famiglia: StateFlow<Famiglia?> = preferenze.famiglia

    suspend fun accedi(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    /** Crea l'account ed entra subito (su Supabase la conferma via email è disattivata). */
    suspend fun registrati(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    /** Legge dal server la famiglia dell'utente (se ne ha già una, es. dopo un nuovo login). */
    suspend fun aggiornaFamiglia() {
        val famiglia = supabase.from("famiglie").select().decodeSingleOrNull<Famiglia>()
        preferenze.salvaFamiglia(famiglia)
        if (famiglia != null) sincronizzazione.richiedi()
    }

    suspend fun creaFamiglia() {
        preferenze.salvaFamiglia(supabase.postgrest.rpc("crea_famiglia").decodeAs<Famiglia>())
        sincronizzazione.richiedi()
    }

    suspend fun unisciti(codice: String) {
        val parametri = buildJsonObject { put("codice", codice.trim()) }
        preferenze.salvaFamiglia(supabase.postgrest.rpc("unisciti_famiglia", parametri).decodeAs<Famiglia>())
        sincronizzazione.richiedi()
    }

    /** Esce e cancella i dati dal telefono: al prossimo accesso si riscaricano dal server. */
    suspend fun esci() {
        sincronizzazione.annulla()
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            // Offline: il server non si raggiunge, ma la sessione locale va chiusa comunque.
            supabase.auth.clearSession()
        }
        turnoDao.svuota()
        preferenze.azzera()
    }
}
