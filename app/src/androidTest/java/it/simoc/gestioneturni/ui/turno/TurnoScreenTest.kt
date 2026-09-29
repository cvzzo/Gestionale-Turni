package it.simoc.gestioneturni.ui.turno

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.simoc.gestioneturni.data.TipoTurno
import it.simoc.gestioneturni.data.local.database.Turno
import it.simoc.gestioneturni.data.nuovoTurno
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.YearMonth

/**
 * UI tests for [TurnoScreen].
 */
@RunWith(AndroidJUnit4::class)
class TurnoScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val oggi = LocalDate.now()
    private val aggiunti = mutableListOf<Turno>()
    private val eliminati = mutableListOf<Turno>()

    private fun mostra(turniDiOggi: List<Turno> = emptyList()) {
        composeTestRule.setContent {
            TurnoScreen(
                state = TurnoUiState.Success(YearMonth.from(oggi), oggi, turniDiOggi),
                snackbarHostState = SnackbarHostState(),
                onGiornoClick = {}, onCambiaMese = {}, onAggiungi = { aggiunti += it },
                onAggiorna = {}, onElimina = { eliminati += it },
            )
        }
    }

    @Test
    fun pulsanteRapido_aggiungeIlTurnoConOrariStandard() {
        mostra()
        composeTestRule.onNodeWithText("Mattina").performClick()

        assertEquals(1, aggiunti.size)
        assertEquals(TipoTurno.MATTINA.inizioStandard, aggiunti.single().inizio)
        assertEquals(TipoTurno.MATTINA.fineStandard, aggiunti.single().fine)
    }

    @Test
    fun straordinario_apreIlDialog() {
        mostra()
        composeTestRule.onNodeWithText("+ Straordinario").performClick()

        composeTestRule.onNodeWithText("Nuovo turno").assertExists()
        assertEquals(0, aggiunti.size)
    }

    @Test
    fun pulsanteRapido_sulTurnoGiaPresente_loToglie() {
        val mattina = nuovoTurno(TipoTurno.MATTINA, oggi, emptyList()).copy(id = "7")
        mostra(listOf(mattina))

        composeTestRule.onNodeWithText("✓ Mattina").performClick()

        assertEquals(listOf(mattina), eliminati)
        assertEquals(0, aggiunti.size)
    }
}
