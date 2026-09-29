package it.simoc.gestioneturni.ui.turno

import it.simoc.gestioneturni.data.TipoTurno
import it.simoc.gestioneturni.data.di.FakeSincronizzazione
import it.simoc.gestioneturni.data.di.FakeTurnoRepository
import it.simoc.gestioneturni.data.nuovoTurno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class TurnoViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initiallyLoading() = runTest {
        val viewModel = TurnoViewModel(FakeTurnoRepository(), FakeSincronizzazione())
        assertEquals(TurnoUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun aggiungi_ilTurnoCompareNelGiornoSelezionato() = runTest {
        val viewModel = TurnoViewModel(FakeTurnoRepository(), FakeSincronizzazione())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        val giorno = LocalDate.now().withDayOfMonth(1).plusMonths(2)

        viewModel.selezionaGiorno(giorno)
        viewModel.aggiungi(nuovoTurno(TipoTurno.NOTTE, giorno, emptyList()))

        val stato = viewModel.uiState.value as TurnoUiState.Success
        assertEquals(giorno, stato.giornoSelezionato)
        assertEquals(listOf(TipoTurno.NOTTE), stato.turniDelGiorno.map { it.tipo })
        assertTrue(viewModel.eventi.first() is Evento.Aggiunto)
    }

    @Test
    fun cambiaMese_selezionaIlPrimoDelMese() = runTest {
        val viewModel = TurnoViewModel(FakeTurnoRepository(), FakeSincronizzazione())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.cambiaMese(1)

        val stato = viewModel.uiState.value as TurnoUiState.Success
        assertEquals(YearMonth.now().plusMonths(1), stato.mese)
        assertEquals(1, stato.giornoSelezionato.dayOfMonth)
    }
}
