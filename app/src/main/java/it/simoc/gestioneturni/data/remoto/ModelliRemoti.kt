package it.simoc.gestioneturni.data.remoto

import it.simoc.gestioneturni.data.TipoTurno
import it.simoc.gestioneturni.data.local.database.Turno
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Riga della tabella `turni` su Supabase (vedi supabase/schema.sql). */
@Serializable
data class TurnoRemoto(
    val id: String,
    @SerialName("famiglia_id") val famigliaId: String,
    val data: String,
    val tipo: String,
    val inizio: Int,
    val fine: Int,
    val note: String,
    @SerialName("modificato_il") val modificatoIl: Long,
    val eliminato: Boolean,
    /** Scritto solo dal server (trigger): serve come cursore per scaricare le novità. */
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** Riga della tabella `famiglie`: il calendario condiviso e il codice per invitare l'altra persona. */
@Serializable
data class Famiglia(
    val id: String,
    @SerialName("codice_invito") val codiceInvito: String,
)

fun Turno.remoto(famigliaId: String) = TurnoRemoto(
    id = id,
    famigliaId = famigliaId,
    data = data.toString(),
    tipo = tipo.name,
    inizio = inizio,
    fine = fine,
    note = note,
    modificatoIl = modificatoIl,
    eliminato = eliminato,
)

/** null se il tipo non è noto a questa versione dell'app (es. aggiunto da una versione più nuova). */
fun TurnoRemoto.locale(): Turno? {
    val tipo = TipoTurno.entries.firstOrNull { it.name == tipo } ?: return null
    return Turno(
        id = id,
        data = LocalDate.parse(data),
        tipo = tipo,
        inizio = inizio,
        fine = fine,
        note = note,
        modificatoIl = modificatoIl,
        eliminato = eliminato,
        daSincronizzare = false,
    )
}

/**
 * Sceglie quali turni scaricati scrivere nel database locale: una modifica locale non ancora
 * inviata e più recente di quella del server ha la precedenza.
 */
fun turniDaApplicare(remoti: List<Turno>, locali: Map<String, Turno>): List<Turno> =
    remoti.filter { remoto ->
        val locale = locali[remoto.id]
        locale == null || !locale.daSincronizzare || remoto.modificatoIl >= locale.modificatoIl
    }
