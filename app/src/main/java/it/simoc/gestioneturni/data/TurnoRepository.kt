package it.simoc.gestioneturni.data

import it.simoc.gestioneturni.data.local.database.Turno
import it.simoc.gestioneturni.data.local.database.TurnoDao
import it.simoc.gestioneturni.data.sync.Sincronizzazione
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

interface TurnoRepository {
    fun turni(da: LocalDate, a: LocalDate): Flow<List<Turno>>

    /** Inserisce (id vuoto) o aggiorna il turno e lo restituisce con l'id definitivo. */
    suspend fun salva(turno: Turno): Turno

    suspend fun elimina(turno: Turno)
}

/**
 * Il database locale è la fonte dei dati per l'interfaccia, così l'app funziona anche offline.
 * Ogni modifica viene segnata "da sincronizzare" e inviata a Supabase appena possibile.
 */
class DefaultTurnoRepository @Inject constructor(
    private val turnoDao: TurnoDao,
    private val sincronizzazione: Sincronizzazione,
) : TurnoRepository {

    override fun turni(da: LocalDate, a: LocalDate): Flow<List<Turno>> = turnoDao.getTurni(da, a)

    override suspend fun salva(turno: Turno): Turno {
        val salvato = turno.copy(
            id = turno.id.ifEmpty { UUID.randomUUID().toString() },
            eliminato = false,
        ).modificatoOra()
        turnoDao.upsert(listOf(salvato))
        sincronizzazione.richiedi()
        return salvato
    }

    override suspend fun elimina(turno: Turno) {
        turnoDao.upsert(listOf(turno.copy(eliminato = true).modificatoOra()))
        sincronizzazione.richiedi()
    }

    // Sempre crescente per lo stesso turno, anche se l'orologio del telefono torna indietro.
    private fun Turno.modificatoOra() = copy(
        modificatoIl = maxOf(System.currentTimeMillis(), modificatoIl + 1),
        daSincronizzare = true,
    )
}
