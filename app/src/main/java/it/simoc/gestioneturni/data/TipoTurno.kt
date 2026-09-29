package it.simoc.gestioneturni.data

/**
 * Tipi di turno. Gli orari sono in minuti dalla mezzanotte e servono solo come valore
 * iniziale: ogni turno salvato ha i propri orari, modificabili senza toccare questi.
 */
enum class TipoTurno(
    val etichetta: String,
    val sigla: String,
    val inizioStandard: Int,
    val fineStandard: Int,
    /** Occupa tutto il giorno senza orari né ore lavorate (riposo, ferie). */
    val giornaliero: Boolean = false,
) {
    MATTINA("Mattina", "M", 7 * 60 + 48, 15 * 60 + 36),
    POMERIGGIO("Pomeriggio", "P", 16 * 60 + 12, 0),
    NOTTE("Notte", "N", 0, 7 * 60 + 48),
    STRAORDINARIO("Straordinario", "S", 8 * 60, 9 * 60),
    RIPOSO("Riposo", "R", 0, 0, giornaliero = true),
    FERIE("Ferie", "F", 0, 0, giornaliero = true),
}

/** Durata di un turno ordinario: 7h 48m. Oltre questa soglia le ore contano come straordinario. */
const val DURATA_TURNO_STANDARD = 7 * 60 + 48
