package it.simoc.gestioneturni.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import it.simoc.gestioneturni.ui.account.AccessoScreen
import it.simoc.gestioneturni.ui.account.AccountViewModel
import it.simoc.gestioneturni.ui.account.CaricamentoScreen
import it.simoc.gestioneturni.ui.account.FamigliaScreen
import it.simoc.gestioneturni.ui.account.NonConfiguratoScreen
import it.simoc.gestioneturni.ui.account.StatoApp
import it.simoc.gestioneturni.ui.turno.TurnoScreen
import it.simoc.gestioneturni.ui.turno.TurnoViewModel

@Composable
fun MainNavigation(accountViewModel: AccountViewModel = hiltViewModel()) {
    val stato by accountViewModel.stato.collectAsStateWithLifecycle()
    val operazione by accountViewModel.operazione.collectAsStateWithLifecycle()

    when (val s = stato) {
        StatoApp.Caricamento -> CaricamentoScreen()
        StatoApp.NonConfigurato -> NonConfiguratoScreen()
        StatoApp.Accesso -> AccessoScreen(
            operazione = operazione,
            onAccedi = accountViewModel::accedi,
            onRegistrati = accountViewModel::registrati,
        )
        StatoApp.SceltaFamiglia -> FamigliaScreen(
            operazione = operazione,
            onCrea = accountViewModel::creaFamiglia,
            onUnisciti = accountViewModel::unisciti,
            onControlla = accountViewModel::controllaFamiglia,
            onEsci = accountViewModel::esci,
        )
        is StatoApp.Pronto -> {
            val backStack = rememberNavBackStack(Main)
            NavDisplay(
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                entryProvider = entryProvider {
                    entry<Main> {
                        TurnoScreen(
                            codiceInvito = s.famiglia.codiceInvito,
                            email = s.email,
                            onEsci = accountViewModel::esci,
                            // Un ViewModel per famiglia: dopo un cambio account non resta quello vecchio.
                            viewModel = hiltViewModel<TurnoViewModel>(key = s.famiglia.id),
                        )
                    }
                }
            )
        }
    }
}
