package it.simoc.gestioneturni.data

import it.simoc.gestioneturni.data.di.FakeSincronizzazione
import it.simoc.gestioneturni.data.local.database.Turno
import it.simoc.gestioneturni.data.local.database.TurnoDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [DefaultTurnoRepository].
 */
class DefaultTurnoRepositoryTest {

    private val giorno = LocalDate.of(2026, 9, 29)
    private val dao = FakeTurnoDao()
    private val sincronizzazione = FakeSincronizzazione()
    private val repository = DefaultTurnoRepository(dao, sincronizzazione)

    @Test
    fun salva_nuovoTurno_riceveUnIdEVaSincronizzato() = runTest {
        val salvato = repository.salva(nuovoTurno(TipoTurno.MATTINA, giorno, emptyList()))

        assertFalse(salvato.nuovo)
        assertTrue(salvato.daSincronizzare)
        assertEquals(listOf(salvato), repository.turni(giorno, giorno).first())
        assertEquals(1, sincronizzazione.richieste)
    }

    @Test
    fun salva_turnoEsistente_loAggiornaConUnaDataDiModificaPiuRecente() = runTest {
        val salvato = repository.salva(nuovoTurno(TipoTurno.MATTINA, giorno, emptyList()))

        val aggiornato = repository.salva(salvato.copy(fine = 17 * 60))

        assertEquals(listOf(17 * 60), repository.turni(giorno, giorno).first().map { it.fine })
        assertTrue(aggiornato.modificatoIl > salvato.modificatoIl)
    }

    @Test
    fun elimina_loNascondeMaLoTieneFinoAllaSincronizzazione() = runTest {
        val salvato = repository.salva(nuovoTurno(TipoTurno.NOTTE, giorno, emptyList()))

        repository.elimina(salvato)

        assertEquals(emptyList<Turno>(), repository.turni(giorno, giorno).first())
        assertTrue(dao.daSincronizzare().single().eliminato)
    }

    @Test
    fun ripristino_dopoElimina_riportaLoStessoTurno() = runTest {
        val salvato = repository.salva(nuovoTurno(TipoTurno.NOTTE, giorno, emptyList()))
        repository.elimina(salvato)

        repository.salva(salvato)

        assertEquals(listOf(salvato.id), repository.turni(giorno, giorno).first().map { it.id })
    }
}

private class FakeTurnoDao : TurnoDao {

    private val data = MutableStateFlow(emptyMap<String, Turno>())

    override fun getTurni(da: LocalDate, a: LocalDate): Flow<List<Turno>> =
        data.map { mappa -> mappa.values.filter { it.data in da..a && !it.eliminato } }

    override suspend fun upsert(turni: List<Turno>) {
        data.update { mappa -> mappa + turni.associateBy { it.id } }
    }

    override suspend fun perId(ids: List<String>): List<Turno> = ids.mapNotNull { data.value[it] }

    override suspend fun daSincronizzare(): List<Turno> = data.value.values.filter { it.daSincronizzare }

    override fun numeroDaSincronizzare(): Flow<Int> = data.map { mappa -> mappa.values.count { it.daSincronizzare } }

    override suspend fun segnaSincronizzato(id: String, modificatoIl: Long) {
        data.update { mappa ->
            val turno = mappa[id]
            if (turno?.modificatoIl == modificatoIl) mappa + (id to turno.copy(daSincronizzare = false)) else mappa
        }
    }

    override suspend fun rimuoviEliminatiSincronizzati() {
        data.update { mappa -> mappa.filterValues { !(it.eliminato && !it.daSincronizzare) } }
    }

    override suspend fun svuota() {
        data.value = emptyMap()
    }
}
