package it.simoc.gestioneturni.data.sync

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import it.simoc.gestioneturni.data.local.database.TurnoDao
import it.simoc.gestioneturni.data.remoto.TurnoRemoto
import it.simoc.gestioneturni.data.remoto.locale
import it.simoc.gestioneturni.data.remoto.remoto
import it.simoc.gestioneturni.data.remoto.turniDaApplicare
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

private const val TABELLA = "turni"
private const val PAGINA = 500L

/**
 * Allinea il database locale con Supabase: prima invia le modifiche fatte sul telefono,
 * poi scarica quelle arrivate dall'altro telefono.
 */
@Singleton
class Sincronizzatore @Inject constructor(
    private val supabase: SupabaseClient,
    private val turnoDao: TurnoDao,
    private val preferenze: PreferenzeSync,
) {
    private val mutex = Mutex()

    suspend fun sincronizza() = mutex.withLock {
        supabase.auth.awaitInitialization()
        if (supabase.auth.currentSessionOrNull() == null) return@withLock
        val famigliaId = preferenze.famiglia.value?.id ?: return@withLock

        invia(famigliaId)
        ricevi(famigliaId)
        turnoDao.rimuoviEliminatiSincronizzati()
    }

    private suspend fun invia(famigliaId: String) {
        turnoDao.daSincronizzare().chunked(PAGINA.toInt()).forEach { blocco ->
            supabase.from(TABELLA).upsert(blocco.map { it.remoto(famigliaId) })
            blocco.forEach { turnoDao.segnaSincronizzato(it.id, it.modificatoIl) }
        }
    }

    private suspend fun ricevi(famigliaId: String) {
        var cursore = preferenze.cursore
        // Un po' di sovrapposizione: una scrittura può finire sul server con un updated_at di poco
        // precedente a una già scaricata. Riscaricare qualche riga non fa danni.
        val da = cursore?.minus(Duration.ofMinutes(2))
        var inizio = 0L
        while (true) {
            val remoti = supabase.from(TABELLA).select {
                filter {
                    eq("famiglia_id", famigliaId)
                    if (da != null) gt("updated_at", da.toString())
                }
                order("updated_at", Order.ASCENDING)
                range(inizio, inizio + PAGINA - 1)
            }.decodeList<TurnoRemoto>()

            applica(remoti)
            remoti.mapNotNull { it.updatedAt?.let { t -> OffsetDateTime.parse(t).toInstant() } }
                .maxOrNull()
                ?.let { ultimo -> if (cursore == null || ultimo > cursore) cursore = ultimo }

            if (remoti.size < PAGINA) break
            inizio += PAGINA
        }
        preferenze.cursore = cursore
    }

    private suspend fun applica(remoti: List<TurnoRemoto>) {
        if (remoti.isEmpty()) return
        val locali = turnoDao.perId(remoti.map { it.id }).associateBy { it.id }
        turnoDao.upsert(turniDaApplicare(remoti.mapNotNull { it.locale() }, locali))
    }
}
