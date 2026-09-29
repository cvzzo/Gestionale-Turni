package it.simoc.gestioneturni.data

import it.simoc.gestioneturni.data.local.database.Turno
import java.time.LocalDate
import java.util.Locale

private const val MINUTI_GIORNO = 24 * 60

/** Durata in minuti; gestisce i turni che scavallano la mezzanotte. */
val Turno.durataMinuti: Int
    get() = (fine - inizio).mod(MINUTI_GIORNO)

/** Uno straordinario conta tutto; un turno normale conta solo quello che supera le 7h 48m. */
val Turno.straordinarioMinuti: Int
    get() = if (tipo == TipoTurno.STRAORDINARIO) {
        durataMinuti
    } else {
        (durataMinuti - DURATA_TURNO_STANDARD).coerceAtLeast(0)
    }

/** Crea un turno per [giorno] con gli orari standard del [tipo]. */
fun nuovoTurno(tipo: TipoTurno, giorno: LocalDate, turniDelGiorno: List<Turno>): Turno {
    if (tipo != TipoTurno.STRAORDINARIO) {
        return Turno(data = giorno, tipo = tipo, inizio = tipo.inizioStandard, fine = tipo.fineStandard)
    }
    // Lo straordinario di solito attacca alla fine dell'ultimo turno della giornata.
    val inizio = turniDelGiorno.filterNot { it.tipo.giornaliero }
        .maxByOrNull { it.inizio }?.fine?.takeIf { it != 0 } ?: tipo.inizioStandard
    return Turno(data = giorno, tipo = tipo, inizio = inizio, fine = (inizio + 60).mod(MINUTI_GIORNO))
}

data class Riepilogo(
    val numeroTurni: Int,
    val minutiTotali: Int,
    val minutiStraordinario: Int,
    val giorniRiposo: Int,
    val giorniFerie: Int,
)

fun List<Turno>.riepilogo() = Riepilogo(
    numeroTurni = count { it.tipo != TipoTurno.STRAORDINARIO && !it.tipo.giornaliero },
    minutiTotali = sumOf { it.durataMinuti },
    minutiStraordinario = sumOf { it.straordinarioMinuti },
    // Contati per giorno, così una voce doppia per sbaglio non conta due volte.
    giorniRiposo = filter { it.tipo == TipoTurno.RIPOSO }.distinctBy { it.data }.size,
    giorniFerie = filter { it.tipo == TipoTurno.FERIE }.distinctBy { it.data }.size,
)

fun formattaOra(minuti: Int): String =
    String.format(Locale.ROOT, "%02d:%02d", minuti / 60 % 24, minuti % 60)

fun formattaDurata(minuti: Int): String =
    String.format(Locale.ROOT, "%dh %02dm", minuti / 60, minuti % 60)
