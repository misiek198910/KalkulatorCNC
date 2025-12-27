import android.content.Context
import com.example.calkulatorcnc.R

data class MaterialParam(
    val name: String,
    val vcMilling: Double,   // Frezowanie
    val fzMilling: Double,   // Posuw na ząb (Frezowanie)
    val vcTurning: Double,   // Toczenie
    val fnTurning: Double,   // Posuw na obrót (Toczenie)
    val vcThreading: Double, // Gwintowanie nożem (Toczenie)
    val vcTapping: Double,   // Gwintowanie gwintownikiem (Frezowanie)
    val vcParting: Double,   // Przecinanie
    val fnParting: Double,
    val vcSawing: Double,    // Piły
    val kc: Double           // Opór właściwy (do mocy)
)

fun getMaterialsList(context: Context): List<MaterialParam> {
    return listOf(
        // Nazwa, vcMill, fzMill, vcTurn, fnTurn, vcThread, vcTap, vcPart, fnPart, vcSaw, kc
        MaterialParam(context.getString(R.string.carbon_steel), 180.0, 0.12, 160.0, 0.25, 100.0, 15.0, 100.0, 0.08, 70.0, 2100.0),
        MaterialParam(context.getString(R.string.alloy_steel), 140.0, 0.10, 130.0, 0.20, 80.0, 12.0, 80.0, 0.06, 50.0, 2400.0),
        MaterialParam(context.getString(R.string.stainless_steel), 80.0, 0.05, 90.0, 0.15, 60.0, 8.0, 50.0, 0.04, 30.0, 2500.0),
        MaterialParam(context.getString(R.string.aluminium), 450.0, 0.20, 400.0, 0.30, 180.0, 30.0, 220.0, 0.15, 150.0, 700.0),
        MaterialParam(context.getString(R.string.cast_iron), 150.0, 0.15, 140.0, 0.25, 90.0, 10.0, 70.0, 0.10, 60.0, 1200.0),
        MaterialParam(context.getString(R.string.titanium), 45.0, 0.04, 50.0, 0.12, 40.0, 5.0, 30.0, 0.03, 15.0, 2800.0),
        MaterialParam(context.getString(R.string.brass), 250.0, 0.15, 220.0, 0.20, 120.0, 20.0, 150.0, 0.10, 90.0, 800.0),
        MaterialParam(context.getString(R.string.hardened_steel), 50.0, 0.04, 45.0, 0.10, 30.0, 4.0, 25.0, 0.02, 10.0, 4500.0)
    )
}