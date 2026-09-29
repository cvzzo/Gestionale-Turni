package it.simoc.gestioneturni.ui.theme

import androidx.compose.ui.graphics.Color
import it.simoc.gestioneturni.data.TipoTurno

data class ColoriTurno(val sfondo: Color, val testo: Color)

/** Colori fissi per tipo, uguali in tema chiaro e scuro così si riconoscono a colpo d'occhio. */
val TipoTurno.colori: ColoriTurno
    get() = when (this) {
        TipoTurno.MATTINA -> ColoriTurno(Color(0xFFFFC857), Color(0xFF3A2A00))
        TipoTurno.POMERIGGIO -> ColoriTurno(Color(0xFF5AA9E6), Color(0xFF002238))
        TipoTurno.NOTTE -> ColoriTurno(Color(0xFF3F3D8F), Color.White)
        TipoTurno.STRAORDINARIO -> ColoriTurno(Color(0xFFD64545), Color.White)
        TipoTurno.RIPOSO -> ColoriTurno(Color(0xFFB4BCC4), Color(0xFF1E2328))
        TipoTurno.FERIE -> ColoriTurno(Color(0xFF43B581), Color(0xFF002A14))
    }
