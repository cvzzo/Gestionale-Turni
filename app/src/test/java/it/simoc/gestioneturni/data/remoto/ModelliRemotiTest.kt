package it.simoc.gestioneturni.data.remoto

import it.simoc.gestioneturni.data.TipoTurno
import it.simoc.gestioneturni.data.nuovoTurno
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ModelliRemotiTest {

    private val turno = nuovoTurno(TipoTurno.POMERIGGIO, LocalDate.of(2026, 9, 30), emptyList())
        .copy(id = "abc", note = "Esteso", fine = 60, modificatoIl = 1_000)

    @Test
    fun andataERitorno_conservanoIlTurno() {
        val remoto = turno.remoto("famiglia")

        assertEquals("2026-09-30", remoto.data)
        assertEquals("POMERIGGIO", remoto.tipo)
        assertEquals(turno, remoto.locale())
    }

    @Test
    fun tipoSconosciuto_vieneIgnorato() {
        assertNull(turno.remoto("famiglia").copy(tipo = "MALATTIA").locale())
    }

    @Test
    fun conflitto_vinceLaModificaPiuRecente() {
        val localeInAttesa = turno.copy(modificatoIl = 2_000, daSincronizzare = true)
        val remotoVecchio = turno.copy(modificatoIl = 1_500)
        val remotoNuovo = turno.copy(modificatoIl = 2_500)

        assertEquals(emptyList<Any>(), turniDaApplicare(listOf(remotoVecchio), mapOf("abc" to localeInAttesa)))
        assertEquals(listOf(remotoNuovo), turniDaApplicare(listOf(remotoNuovo), mapOf("abc" to localeInAttesa)))
    }

    @Test
    fun turnoLocaleGiaSincronizzato_vieneSempreAggiornatoDalServer() {
        val localeSincronizzato = turno.copy(modificatoIl = 9_000)
        val remoto = turno.copy(modificatoIl = 1_000, note = "Dal server")

        assertEquals(listOf(remoto), turniDaApplicare(listOf(remoto), mapOf("abc" to localeSincronizzato)))
    }
}
