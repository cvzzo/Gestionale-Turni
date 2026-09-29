package it.simoc.gestioneturni.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import it.simoc.gestioneturni.data.DefaultTurnoRepository
import it.simoc.gestioneturni.data.TipoTurno
import it.simoc.gestioneturni.data.TurnoRepository
import it.simoc.gestioneturni.data.local.database.Turno
import it.simoc.gestioneturni.data.nuovoTurno
import it.simoc.gestioneturni.data.sync.DefaultSincronizzazione
import it.simoc.gestioneturni.data.sync.Sincronizzazione
import it.simoc.gestioneturni.data.sync.StatoSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Singleton
    @Binds
    fun bindsTurnoRepository(
        turnoRepository: DefaultTurnoRepository
    ): TurnoRepository
}

// Separato da DataModule: i test strumentati sostituiscono solo il repository dei turni.
@Module
@InstallIn(SingletonComponent::class)
interface SyncModule {

    @Binds
    fun bindsSincronizzazione(
        sincronizzazione: DefaultSincronizzazione
    ): Sincronizzazione
}

class FakeTurnoRepository @Inject constructor() : TurnoRepository {
    private val dati = MutableStateFlow(fakeTurni)

    override fun turni(da: LocalDate, a: LocalDate): Flow<List<Turno>> =
        dati.map { lista -> lista.filter { it.data in da..a } }

    override suspend fun salva(turno: Turno): Turno {
        val salvato = if (turno.nuovo) turno.copy(id = UUID.randomUUID().toString()) else turno
        dati.update { lista -> lista.filter { it.id != salvato.id } + salvato }
        return salvato
    }

    override suspend fun elimina(turno: Turno) {
        dati.update { lista -> lista.filter { it.id != turno.id } }
    }
}

class FakeSincronizzazione @Inject constructor() : Sincronizzazione {
    var richieste = 0
        private set

    override val stato: Flow<StatoSync> = flowOf(StatoSync())

    override fun richiedi() {
        richieste++
    }

    override fun modificheRemote(): Flow<Unit> = emptyFlow()

    override fun annulla() = Unit
}

val fakeTurni: List<Turno> = LocalDate.now().let { oggi ->
    listOf(
        nuovoTurno(TipoTurno.MATTINA, oggi, emptyList()).copy(id = "1"),
        nuovoTurno(TipoTurno.POMERIGGIO, oggi.plusDays(1), emptyList()).copy(id = "2", fine = 60, note = "Esteso"),
        nuovoTurno(TipoTurno.NOTTE, oggi.plusDays(3), emptyList()).copy(id = "3"),
    )
}
