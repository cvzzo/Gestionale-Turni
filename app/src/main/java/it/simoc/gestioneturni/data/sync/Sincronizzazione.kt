package it.simoc.gestioneturni.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import it.simoc.gestioneturni.data.local.database.TurnoDao
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

data class StatoSync(
    val inCorso: Boolean = false,
    /** Modifiche fatte sul telefono e non ancora arrivate al server (es. perché offline). */
    val modificheInAttesa: Int = 0,
)

interface Sincronizzazione {
    val stato: Flow<StatoSync>

    /** Pianifica una sincronizzazione, che parte appena c'è rete. */
    fun richiedi()

    /** Emette ogni volta che l'altro telefono modifica un turno (Supabase Realtime). */
    fun modificheRemote(): Flow<Unit>

    fun annulla()
}

private const val NOME_LAVORO = "sincronizzazione_turni"
private const val TAG = "Sincronizzazione"

@Singleton
class DefaultSincronizzazione @Inject constructor(
    @ApplicationContext private val context: Context,
    private val supabase: SupabaseClient,
    private val turnoDao: TurnoDao,
    private val preferenze: PreferenzeSync,
) : Sincronizzazione {

    private val workManager get() = WorkManager.getInstance(context)

    override val stato: Flow<StatoSync> = combine(
        turnoDao.numeroDaSincronizzare(),
        workManager.getWorkInfosForUniqueWorkFlow(NOME_LAVORO),
    ) { inAttesa, lavori ->
        StatoSync(inCorso = lavori.any { it.state == WorkInfo.State.RUNNING }, modificheInAttesa = inAttesa)
    }

    override fun richiedi() {
        val richiesta = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // In coda a quella in corso: così una modifica fatta durante una sincronizzazione non si perde.
        workManager.enqueueUniqueWork(NOME_LAVORO, ExistingWorkPolicy.APPEND_OR_REPLACE, richiesta)
    }

    override fun modificheRemote(): Flow<Unit> = flow {
        val famigliaId = preferenze.famiglia.value?.id ?: return@flow
        val canale = supabase.channel("turni-$famigliaId")
        val modifiche = canale.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "turni"
            filter("famiglia_id", FilterOperator.EQ, famigliaId)
        }
        canale.subscribe()
        try {
            modifiche.collect { emit(Unit) }
        } finally {
            withContext(NonCancellable) { supabase.realtime.removeChannel(canale) }
        }
    }

    override fun annulla() {
        workManager.cancelUniqueWork(NOME_LAVORO)
    }
}

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sincronizzatore: Sincronizzatore,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        sincronizzatore.sincronizza()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Sincronizzazione fallita (tentativo $runAttemptCount)", e)
        if (runAttemptCount < 5) Result.retry() else Result.failure()
    }
}
