package it.simoc.gestioneturni.data

import it.simoc.gestioneturni.data.local.database.Turno
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TurnoCalcoliTest {

    private val giorno = LocalDate.of(2026, 9, 29)

    @Test
    fun turniStandard_duranoSetteOreEQuarantotto_senzaStraordinario() {
        listOf(TipoTurno.MATTINA, TipoTurno.POMERIGGIO, TipoTurno.NOTTE).forEach { tipo ->
            val turno = nuovoTurno(tipo, giorno, emptyList())
            assertEquals(tipo.name, DURATA_TURNO_STANDARD, turno.durataMinuti)
            assertEquals(tipo.name, 0, turno.straordinarioMinuti)
        }
    }

    @Test
    fun pomeriggioEstesoOltreMezzanotte_contaLoStraordinario() {
        val turno = nuovoTurno(TipoTurno.POMERIGGIO, giorno, emptyList()).copy(fine = 90)
        assertEquals(DURATA_TURNO_STANDARD + 90, turno.durataMinuti)
        assertEquals(90, turno.straordinarioMinuti)
    }

    @Test
    fun mattinaFinitaPrima_nonHaStraordinarioNegativo() {
        val turno = nuovoTurno(TipoTurno.MATTINA, giorno, emptyList()).copy(fine = 12 * 60)
        assertEquals(0, turno.straordinarioMinuti)
    }

    @Test
    fun straordinario_contaTuttaLaDurata_eParteDopoLUltimoTurno() {
        val mattina = nuovoTurno(TipoTurno.MATTINA, giorno, emptyList())
        val extra = nuovoTurno(TipoTurno.STRAORDINARIO, giorno, listOf(mattina))
        assertEquals(mattina.fine, extra.inizio)
        assertEquals(60, extra.straordinarioMinuti)
    }

    @Test
    fun riepilogo_sommaOreEStraordinari() {
        val turni = listOf(
            nuovoTurno(TipoTurno.MATTINA, giorno, emptyList()).copy(fine = 16 * 60 + 36),
            Turno(data = giorno, tipo = TipoTurno.STRAORDINARIO, inizio = 18 * 60, fine = 20 * 60),
        )
        val riepilogo = turni.riepilogo()
        assertEquals(1, riepilogo.numeroTurni)
        assertEquals(DURATA_TURNO_STANDARD + 60 + 120, riepilogo.minutiTotali)
        assertEquals(180, riepilogo.minutiStraordinario)
    }

    @Test
    fun riposoEFerie_nonContanoOre_maSiContanoIGiorni() {
        val turni = listOf(
            nuovoTurno(TipoTurno.RIPOSO, giorno, emptyList()),
            nuovoTurno(TipoTurno.FERIE, giorno.plusDays(1), emptyList()),
            nuovoTurno(TipoTurno.FERIE, giorno.plusDays(2), emptyList()),
        )
        val riepilogo = turni.riepilogo()
        assertEquals(0, riepilogo.numeroTurni)
        assertEquals(0, riepilogo.minutiTotali)
        assertEquals(0, riepilogo.minutiStraordinario)
        assertEquals(1, riepilogo.giorniRiposo)
        assertEquals(2, riepilogo.giorniFerie)
    }

    @Test
    fun straordinario_ignoraRiposoEFerieNelCalcoloDellInizio() {
        val riposo = nuovoTurno(TipoTurno.RIPOSO, giorno, emptyList())
        val extra = nuovoTurno(TipoTurno.STRAORDINARIO, giorno, listOf(riposo))
        assertEquals(TipoTurno.STRAORDINARIO.inizioStandard, extra.inizio)
    }

    @Test
    fun formattazione() {
        assertEquals("07:48", formattaOra(7 * 60 + 48))
        assertEquals("00:00", formattaOra(0))
        assertEquals("7h 48m", formattaDurata(DURATA_TURNO_STANDARD))
    }
}
