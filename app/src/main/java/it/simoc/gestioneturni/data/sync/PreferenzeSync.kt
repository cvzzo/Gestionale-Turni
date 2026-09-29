package it.simoc.gestioneturni.data.sync

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import it.simoc.gestioneturni.data.remoto.Famiglia
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stato della sincronizzazione salvato sul telefono: la famiglia (per aprire l'app anche
 * offline) e fin dove sono già stati scaricati i turni dal server.
 */
@Singleton
class PreferenzeSync @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = context.getSharedPreferences("sincronizzazione", Context.MODE_PRIVATE)

    private val _famiglia = MutableStateFlow(leggiFamiglia())
    val famiglia: StateFlow<Famiglia?> = _famiglia

    fun salvaFamiglia(famiglia: Famiglia?) {
        prefs.edit {
            putString(FAMIGLIA_ID, famiglia?.id)
            putString(CODICE_INVITO, famiglia?.codiceInvito)
        }
        _famiglia.value = famiglia
    }

    /** `updated_at` del server dell'ultimo turno scaricato. */
    var cursore: Instant?
        get() = prefs.getString(CURSORE, null)?.let(Instant::parse)
        set(valore) = prefs.edit { putString(CURSORE, valore?.toString()) }

    fun azzera() {
        prefs.edit { clear() }
        _famiglia.value = null
    }

    private fun leggiFamiglia(): Famiglia? {
        val id = prefs.getString(FAMIGLIA_ID, null) ?: return null
        return Famiglia(id, prefs.getString(CODICE_INVITO, null).orEmpty())
    }

    private companion object {
        const val FAMIGLIA_ID = "famiglia_id"
        const val CODICE_INVITO = "codice_invito"
        const val CURSORE = "cursore"
    }
}
