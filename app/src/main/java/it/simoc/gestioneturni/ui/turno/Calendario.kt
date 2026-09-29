package it.simoc.gestioneturni.ui.turno

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.simoc.gestioneturni.data.TipoTurno
import it.simoc.gestioneturni.data.local.database.Turno
import it.simoc.gestioneturni.data.straordinarioMinuti
import it.simoc.gestioneturni.ui.theme.colori
import java.time.LocalDate
import java.time.YearMonth

private val GIORNI_SETTIMANA = listOf("L", "M", "M", "G", "V", "S", "D")

@Composable
fun Calendario(
    mese: YearMonth,
    giornoSelezionato: LocalDate,
    turni: List<Turno>,
    onGiornoClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val turniPerGiorno = remember(turni) { turni.groupBy { it.data } }
    val oggi = LocalDate.now()
    // La settimana parte dal lunedì: quante celle vuote prima del giorno 1.
    val vuotiIniziali = mese.atDay(1).dayOfWeek.value - 1
    val giorniNelMese = mese.lengthOfMonth()
    val settimane = (vuotiIniziali + giorniNelMese + 6) / 7

    Column(modifier) {
        Row {
            GIORNI_SETTIMANA.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        repeat(settimane) { settimana ->
            Row {
                repeat(7) { colonna ->
                    val numero = settimana * 7 + colonna - vuotiIniziali + 1
                    if (numero in 1..giorniNelMese) {
                        val data = mese.atDay(numero)
                        CellaGiorno(
                            numero = numero,
                            turni = turniPerGiorno[data].orEmpty(),
                            selezionato = data == giornoSelezionato,
                            oggi = data == oggi,
                            onClick = { onGiornoClick(data) },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CellaGiorno(
    numero: Int,
    turni: List<Turno>,
    selezionato: Boolean,
    oggi: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val forma = RoundedCornerShape(10.dp)
    Column(
        modifier = modifier
            .padding(2.dp)
            .height(62.dp)
            .clip(forma)
            .background(if (selezionato) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .then(if (oggi) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, forma) else Modifier)
            .clickable(onClick = onClick)
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = numero.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (oggi) FontWeight.Bold else FontWeight.Normal,
        )
        // Uno straordinario a parte ha già la sua "S": il pallino segnala i turni estesi.
        val turniNormali = turni.filter { it.tipo != TipoTurno.STRAORDINARIO }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            turni.distinctBy { it.tipo }.take(3).forEach { BollinoTurno(it.tipo, dimensione = 16.dp) }
        }
        if (turniNormali.any { it.straordinarioMinuti > 0 }) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(TipoTurno.STRAORDINARIO.colori.sfondo)
            )
        }
    }
}

@Composable
fun BollinoTurno(tipo: TipoTurno, dimensione: Dp, modifier: Modifier = Modifier) {
    val colori = tipo.colori
    Box(
        modifier = modifier
            .size(dimensione)
            .clip(RoundedCornerShape(dimensione / 4))
            .background(colori.sfondo),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tipo.sigla,
            color = colori.testo,
            fontSize = (dimensione.value * 0.6f).sp,
            fontWeight = FontWeight.Bold,
            lineHeight = (dimensione.value * 0.6f).sp,
        )
    }
}
