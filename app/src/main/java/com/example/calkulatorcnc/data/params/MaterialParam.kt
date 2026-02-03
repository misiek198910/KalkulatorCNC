import android.content.Context
import com.example.calkulatorcnc.R

// MaterialParam.kt

data class MaterialParam(
    val name: String,
    val isoGroup: String, // NOWOŚĆ: Grupa ISO (P, M, K, N, S, H)
    val vcMilling: Double,
    val fzMilling: Double,
    val vcTurning: Double,
    val fnTurning: Double,
    val vcThreading: Double,
    val vcTapping: Double,
    val vcParting: Double,
    val fnParting: Double,
    val vcSawing: Double,
    val kc: Double
)

fun getMaterialsList(context: Context): List<MaterialParam> {
    return listOf(
        // Nazwa, Grupa ISO, vcMill, fzMill, vcTurn, fnTurn, vcThread, vcTap, vcPart, fnPart, vcSaw, kc
        MaterialParam(context.getString(R.string.carbon_steel), "P", 180.0, 0.12, 160.0, 0.25, 100.0, 15.0, 100.0, 0.08, 70.0, 2100.0),
        MaterialParam(context.getString(R.string.alloy_steel), "P", 140.0, 0.10, 130.0, 0.20, 80.0, 12.0, 80.0, 0.06, 50.0, 2400.0),
        MaterialParam(context.getString(R.string.stainless_steel), "M", 80.0, 0.05, 90.0, 0.15, 60.0, 8.0, 50.0, 0.04, 30.0, 2500.0),
        MaterialParam(context.getString(R.string.cast_iron), "K", 150.0, 0.15, 140.0, 0.25, 90.0, 10.0, 70.0, 0.10, 60.0, 1200.0),
        MaterialParam(context.getString(R.string.aluminium), "N", 450.0, 0.20, 400.0, 0.30, 180.0, 30.0, 220.0, 0.15, 150.0, 700.0),
        MaterialParam(context.getString(R.string.brass), "N", 250.0, 0.15, 220.0, 0.20, 120.0, 20.0, 150.0, 0.10, 90.0, 800.0),
        MaterialParam(context.getString(R.string.titanium), "S", 45.0, 0.04, 50.0, 0.12, 40.0, 5.0, 30.0, 0.03, 15.0, 2800.0),
        MaterialParam(context.getString(R.string.hardened_steel), "H", 50.0, 0.04, 45.0, 0.10, 30.0, 4.0, 25.0, 0.02, 10.0, 4500.0)
    )
}

/**
 * ZABEZPIECZENIE: Funkcja szuka grupy ISO dla tekstowej nazwy materiału zapisanej w bazie.
 * Jeśli nazwa nie pasuje do żadnej definicji, zwraca pusty ciąg (brak koloru).
 */
fun getIsoGroupForMaterial(context: Context, materialName: String?): String {
    if (materialName.isNullOrBlank()) return ""
    val list = getMaterialsList(context)
    // Szukamy dopasowania ignorując wielkość liter - to obsłuży stare wpisy
    return list.find { it.name.equals(materialName, ignoreCase = true) }?.isoGroup ?: ""
}