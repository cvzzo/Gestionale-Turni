package it.simoc.gestioneturni.data.local.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import androidx.room.Upsert
import it.simoc.gestioneturni.data.TipoTurno
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Un turno lavorato in un giorno. [inizio] e [fine] sono minuti dalla mezzanotte;
 * se [fine] <= [inizio] il turno finisce il giorno dopo (es. Pomeriggio 16:12-00:00).
 *
 * [id] vuoto = turno non ancora salvato; al salvataggio diventa un UUID, così si può creare
 * anche offline e poi inviarlo a Supabase con lo stesso id.
 */
@Entity(tableName = "turno", indices = [Index("data"), Index("daSincronizzare")])
data class Turno(
    @PrimaryKey
    val id: String = "",
    val data: LocalDate,
    val tipo: TipoTurno,
    val inizio: Int,
    val fine: Int,
    val note: String = "",
    /** Ora dell'ultima modifica (ms): in caso di conflitto fra i due telefoni vince la più recente. */
    val modificatoIl: Long = 0,
    /** Eliminazione "soft": resta finché non è stata comunicata al server. */
    val eliminato: Boolean = false,
    /** Modifica locale non ancora inviata a Supabase. */
    val daSincronizzare: Boolean = false,
) {
    val nuovo: Boolean get() = id.isEmpty()
}

@Dao
interface TurnoDao {
    @Query("SELECT * FROM turno WHERE data BETWEEN :da AND :a AND eliminato = 0 ORDER BY data, inizio")
    fun getTurni(da: LocalDate, a: LocalDate): Flow<List<Turno>>

    @Upsert
    suspend fun upsert(turni: List<Turno>)

    @Query("SELECT * FROM turno WHERE id IN (:ids)")
    suspend fun perId(ids: List<String>): List<Turno>

    @Query("SELECT * FROM turno WHERE daSincronizzare = 1")
    suspend fun daSincronizzare(): List<Turno>

    @Query("SELECT COUNT(*) FROM turno WHERE daSincronizzare = 1")
    fun numeroDaSincronizzare(): Flow<Int>

    /** Segna come inviato solo se nel frattempo non è stato modificato di nuovo. */
    @Query("UPDATE turno SET daSincronizzare = 0 WHERE id = :id AND modificatoIl = :modificatoIl")
    suspend fun segnaSincronizzato(id: String, modificatoIl: Long)

    @Query("DELETE FROM turno WHERE eliminato = 1 AND daSincronizzare = 0")
    suspend fun rimuoviEliminatiSincronizzati()

    @Query("DELETE FROM turno")
    suspend fun svuota()
}

class Converters {
    @TypeConverter
    fun daLocalDate(data: LocalDate): Long = data.toEpochDay()

    @TypeConverter
    fun aLocalDate(epochDay: Long): LocalDate = LocalDate.ofEpochDay(epochDay)
}
