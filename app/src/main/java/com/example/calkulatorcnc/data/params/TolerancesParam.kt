package com.example.calkulatorcnc.data.params

data class ToleranceValue(
    val minDia: Double,    // Średnica "powyżej" (exclusive)
    val maxDia: Double,    // Średnica "do" (inclusive)
    val upperUm: Double,   // Odchyłka górna w mikrometrach [µm]
    val lowerUm: Double    // Odchyłka dolna w mikrometrach [µm]
)

data class IsoTolerance(
    val label: String,      // Nazwa np. "H7", "h6", "js7"
    val isHole: Boolean,    // True = Otwór (duża litera), False = Wałek (mała litera)
    val values: List<ToleranceValue>
)

data class FitResult(
    val holeMin: Double,
    val holeMax: Double,
    val shaftMin: Double,
    val shaftMax: Double,
    val type: FitType,
    val maxClearance: Double, // Max luz (dodatnie) lub Max wcisk (ujemne)
    val minClearance: Double
)

enum class FitType {
    CLEARANCE,    // Luźne (zawsze luz)
    TRANSITION,   // Mieszane (może być luz lub wcisk)
    INTERFERENCE  // Ciasne (zawsze wcisk)
}

object ToleranceData {
    fun getIsoTolerances(): List<IsoTolerance> {
        return listOf(
            IsoTolerance(
                "H6", true, listOf(
                    ToleranceValue(0.0, 3.0, 6.0, 0.0),
                    ToleranceValue(3.0, 6.0, 8.0, 0.0),
                    ToleranceValue(6.0, 10.0, 9.0, 0.0),
                    ToleranceValue(10.0, 18.0, 11.0, 0.0),
                    ToleranceValue(18.0, 30.0, 13.0, 0.0),
                    ToleranceValue(30.0, 50.0, 16.0, 0.0),
                    ToleranceValue(50.0, 80.0, 19.0, 0.0),
                    ToleranceValue(80.0, 120.0, 22.0, 0.0),
                    ToleranceValue(120.0, 180.0, 25.0, 0.0),
                    ToleranceValue(180.0, 250.0, 29.0, 0.0),
                    ToleranceValue(250.0, 315.0, 32.0, 0.0),
                    ToleranceValue(315.0, 400.0, 36.0, 0.0),
                    ToleranceValue(400.0, 500.0, 40.0, 0.0)
                )
            ),
            // --- OTWORY (Hole Basis) ---
            IsoTolerance(
                "H7", true, listOf(
                    ToleranceValue(0.0, 3.0, 10.0, 0.0),
                    ToleranceValue(3.0, 6.0, 12.0, 0.0),
                    ToleranceValue(6.0, 10.0, 15.0, 0.0),
                    ToleranceValue(10.0, 18.0, 18.0, 0.0),
                    ToleranceValue(18.0, 30.0, 21.0, 0.0),
                    ToleranceValue(30.0, 50.0, 25.0, 0.0),
                    ToleranceValue(50.0, 80.0, 30.0, 0.0),
                    ToleranceValue(80.0, 120.0, 35.0, 0.0),
                    ToleranceValue(120.0, 180.0, 40.0, 0.0),
                    ToleranceValue(180.0, 250.0, 46.0, 0.0),
                    ToleranceValue(250.0, 315.0, 52.0, 0.0),
                    ToleranceValue(315.0, 400.0, 57.0, 0.0),
                    ToleranceValue(400.0, 500.0, 63.0, 0.0)
                )
            ),
            IsoTolerance(
                "H8", true, listOf(
                    ToleranceValue(0.0, 3.0, 14.0, 0.0),
                    ToleranceValue(3.0, 6.0, 18.0, 0.0),
                    ToleranceValue(6.0, 10.0, 22.0, 0.0),
                    ToleranceValue(10.0, 18.0, 27.0, 0.0),
                    ToleranceValue(18.0, 30.0, 33.0, 0.0),
                    ToleranceValue(30.0, 50.0, 39.0, 0.0),
                    ToleranceValue(50.0, 80.0, 46.0, 0.0),
                    ToleranceValue(80.0, 120.0, 54.0, 0.0),
                    ToleranceValue(120.0, 180.0, 63.0, 0.0),
                    ToleranceValue(180.0, 250.0, 72.0, 0.0),
                    ToleranceValue(250.0, 315.0, 81.0, 0.0),
                    ToleranceValue(315.0, 400.0, 89.0, 0.0),
                    ToleranceValue(400.0, 500.0, 97.0, 0.0)
                )
            ),
            // G7: Pasowanie ruchome, lekkie (części poruszające się z filmem olejowym)
            IsoTolerance(
                "G7", true, listOf(
                    ToleranceValue(0.0, 3.0, 12.0, 2.0),
                    ToleranceValue(3.0, 6.0, 16.0, 4.0),
                    ToleranceValue(6.0, 10.0, 20.0, 5.0),
                    ToleranceValue(10.0, 18.0, 24.0, 6.0),
                    ToleranceValue(18.0, 30.0, 28.0, 7.0),
                    ToleranceValue(30.0, 50.0, 34.0, 9.0),
                    ToleranceValue(50.0, 80.0, 40.0, 10.0),
                    ToleranceValue(80.0, 120.0, 47.0, 12.0),
                    ToleranceValue(120.0, 180.0, 54.0, 14.0),
                    ToleranceValue(180.0, 250.0, 61.0, 15.0),
                    ToleranceValue(250.0, 315.0, 69.0, 17.0),
                    ToleranceValue(315.0, 400.0, 75.0, 18.0),
                    ToleranceValue(400.0, 500.0, 83.0, 20.0)
                )
            ),
            IsoTolerance(
                "F7", true, listOf(
                    ToleranceValue(0.0, 3.0, 16.0, 6.0),
                    ToleranceValue(3.0, 6.0, 22.0, 10.0),
                    ToleranceValue(6.0, 10.0, 28.0, 13.0),
                    ToleranceValue(10.0, 18.0, 34.0, 16.0),
                    ToleranceValue(18.0, 30.0, 41.0, 20.0),
                    ToleranceValue(30.0, 50.0, 50.0, 25.0),
                    ToleranceValue(50.0, 80.0, 60.0, 30.0),
                    ToleranceValue(80.0, 120.0, 71.0, 36.0),
                    ToleranceValue(120.0, 180.0, 83.0, 43.0),
                    ToleranceValue(180.0, 250.0, 96.0, 50.0),
                    ToleranceValue(250.0, 315.0, 108.0, 56.0),
                    ToleranceValue(315.0, 400.0, 119.0, 62.0),
                    ToleranceValue(400.0, 500.0, 131.0, 68.0)
                )
            ),
            // JS7: Tolerancja symetryczna (+/-) - łatwa do wykonania na maszynie
            IsoTolerance(
                "JS7", true, listOf(
                    ToleranceValue(0.0, 3.0, 5.0, -5.0),
                    ToleranceValue(3.0, 6.0, 6.0, -6.0),
                    ToleranceValue(6.0, 10.0, 7.5, -7.5),
                    ToleranceValue(10.0, 18.0, 9.0, -9.0),
                    ToleranceValue(18.0, 30.0, 10.5, -10.5),
                    ToleranceValue(30.0, 50.0, 12.5, -12.5),
                    ToleranceValue(50.0, 80.0, 15.0, -15.0),
                    ToleranceValue(80.0, 120.0, 17.5, -17.5),
                    ToleranceValue(120.0, 180.0, 20.0, -20.0),
                    ToleranceValue(180.0, 250.0, 23.0, -23.0),
                    ToleranceValue(250.0, 315.0, 26.0, -26.0),
                    ToleranceValue(315.0, 400.0, 28.5, -28.5),
                    ToleranceValue(400.0, 500.0, 31.5, -31.5)
                )
            ),
            IsoTolerance(
                "E8", true, listOf(
                    ToleranceValue(0.0, 3.0, 28.0, 14.0),
                    ToleranceValue(3.0, 6.0, 38.0, 20.0),
                    ToleranceValue(6.0, 10.0, 47.0, 25.0),
                    ToleranceValue(10.0, 18.0, 59.0, 32.0),
                    ToleranceValue(18.0, 30.0, 73.0, 40.0),
                    ToleranceValue(30.0, 50.0, 89.0, 50.0),
                    ToleranceValue(50.0, 80.0, 106.0, 60.0),
                    ToleranceValue(80.0, 120.0, 126.0, 72.0),
                    ToleranceValue(120.0, 180.0, 148.0, 85.0),
                    ToleranceValue(180.0, 250.0, 172.0, 100.0),
                    ToleranceValue(250.0, 315.0, 191.0, 110.0),
                    ToleranceValue(315.0, 400.0, 214.0, 125.0),
                    ToleranceValue(400.0, 500.0, 235.0, 135.0)
                )
            ),

            // m6: Pasowanie mieszane/ciasne (łożyska i koła zębate)
            IsoTolerance(
                "m6", false, listOf(
                    ToleranceValue(0.0, 3.0, 8.0, 2.0),
                    ToleranceValue(3.0, 6.0, 12.0, 4.0),
                    ToleranceValue(6.0, 10.0, 15.0, 6.0),
                    ToleranceValue(10.0, 18.0, 18.0, 7.0),
                    ToleranceValue(18.0, 30.0, 21.0, 8.0),
                    ToleranceValue(30.0, 50.0, 25.0, 9.0),
                    ToleranceValue(50.0, 80.0, 30.0, 11.0),
                    ToleranceValue(80.0, 120.0, 35.0, 13.0),
                    ToleranceValue(120.0, 180.0, 40.0, 15.0),
                    ToleranceValue(180.0, 250.0, 46.0, 17.0),
                    ToleranceValue(250.0, 315.0, 52.0, 20.0),
                    ToleranceValue(315.0, 400.0, 57.0, 21.0),
                    ToleranceValue(400.0, 500.0, 63.0, 23.0)
                )
            ),

            IsoTolerance(
                "H11", true, listOf(
                    ToleranceValue(0.0, 3.0, 60.0, 0.0),
                    ToleranceValue(3.0, 6.0, 75.0, 0.0),
                    ToleranceValue(6.0, 10.0, 90.0, 0.0),
                    ToleranceValue(10.0, 18.0, 110.0, 0.0),
                    ToleranceValue(18.0, 30.0, 130.0, 0.0),
                    ToleranceValue(30.0, 50.0, 160.0, 0.0),
                    ToleranceValue(50.0, 80.0, 190.0, 0.0),
                    ToleranceValue(80.0, 120.0, 220.0, 0.0),
                    ToleranceValue(120.0, 180.0, 250.0, 0.0),
                    ToleranceValue(180.0, 250.0, 290.0, 0.0),
                    ToleranceValue(250.0, 315.0, 320.0, 0.0),
                    ToleranceValue(315.0, 400.0, 360.0, 0.0),
                    ToleranceValue(400.0, 500.0, 400.0, 0.0)
                )
            ),

            // P7: Pasowanie ciasne (montaż na prasie, części nieruchome)
            IsoTolerance(
                "P7", true, listOf(
                    ToleranceValue(0.0, 3.0, -6.0, -16.0),
                    ToleranceValue(3.0, 6.0, -8.0, -20.0),
                    ToleranceValue(6.0, 10.0, -9.0, -24.0),
                    ToleranceValue(10.0, 18.0, -11.0, -29.0),
                    ToleranceValue(18.0, 30.0, -14.0, -35.0),
                    ToleranceValue(30.0, 50.0, -17.0, -42.0),
                    ToleranceValue(50.0, 80.0, -21.0, -51.0),
                    ToleranceValue(80.0, 120.0, -24.0, -59.0)
                )
            ),

            // --- WAŁKI (Shaft Basis) ---
            IsoTolerance(
                "h6", false, listOf(
                    ToleranceValue(0.0, 3.0, 0.0, -6.0),
                    ToleranceValue(3.0, 6.0, 0.0, -8.0),
                    ToleranceValue(6.0, 10.0, 0.0, -9.0),
                    ToleranceValue(10.0, 18.0, 0.0, -11.0),
                    ToleranceValue(18.0, 30.0, 0.0, -13.0),
                    ToleranceValue(30.0, 50.0, 0.0, -16.0),
                    ToleranceValue(50.0, 80.0, 0.0, -19.0),
                    ToleranceValue(80.0, 120.0, 0.0, -22.0),
                    ToleranceValue(120.0, 180.0, 0.0, -25.0),
                    ToleranceValue(180.0, 250.0, 0.0, -29.0),
                    ToleranceValue(250.0, 315.0, 0.0, -32.0),
                    ToleranceValue(315.0, 400.0, 0.0, -36.0),
                    ToleranceValue(400.0, 500.0, 0.0, -40.0)
                )
            ),

            // Przykład dla g6 (częsty pasowanie luźne)
            IsoTolerance(
                "g6", false, listOf(
                    ToleranceValue(0.0, 3.0, -2.0, -8.0),
                    ToleranceValue(3.0, 6.0, -4.0, -12.0),
                    ToleranceValue(6.0, 10.0, -5.0, -14.0),
                    ToleranceValue(10.0, 18.0, -6.0, -17.0),
                    ToleranceValue(18.0, 30.0, -7.0, -20.0),
                    ToleranceValue(30.0, 50.0, -9.0, -25.0),
                    ToleranceValue(50.0, 80.0, -10.0, -29.0),
                    ToleranceValue(80.0, 120.0, -12.0, -34.0)
                )
            ),
            // f7: Pasowanie luźne (części obracające się z luzem, osie kół)
            IsoTolerance(
                "f7", false, listOf(
                    ToleranceValue(0.0, 3.0, -6.0, -16.0),
                    ToleranceValue(3.0, 6.0, -10.0, -22.0),
                    ToleranceValue(6.0, 10.0, -13.0, -28.0),
                    ToleranceValue(10.0, 18.0, -16.0, -34.0),
                    ToleranceValue(18.0, 30.0, -20.0, -41.0),
                    ToleranceValue(30.0, 50.0, -25.0, -50.0),
                    ToleranceValue(50.0, 80.0, -30.0, -60.0),
                    ToleranceValue(80.0, 120.0, -36.0, -71.0),
                    ToleranceValue(120.0, 180.0, -43.0, -83.0),
                    ToleranceValue(180.0, 250.0, -50.0, -96.0),
                    ToleranceValue(250.0, 315.0, -56.0, -108.0),
                    ToleranceValue(315.0, 400.0, -62.0, -119.0),
                    ToleranceValue(400.0, 500.0, -68.0, -131.0)
                )
            ),

            // k6: Pasowanie mieszane (lekki wcisk, kołki ustalające)
            IsoTolerance(
                "k6", false, listOf(
                    ToleranceValue(0.0, 3.0, 6.0, 0.0),
                    ToleranceValue(3.0, 6.0, 9.0, 1.0),
                    ToleranceValue(6.0, 10.0, 10.0, 1.0),
                    ToleranceValue(10.0, 18.0, 12.0, 1.0),
                    ToleranceValue(18.0, 30.0, 15.0, 2.0),
                    ToleranceValue(30.0, 50.0, 18.0, 2.0),
                    ToleranceValue(50.0, 80.0, 21.0, 2.0),
                    ToleranceValue(80.0, 120.0, 25.0, 3.0),
                    ToleranceValue(120.0, 180.0, 28.0, 3.0),
                    ToleranceValue(180.0, 250.0, 33.0, 4.0),
                    ToleranceValue(250.0, 315.0, 36.0, 4.0),
                    ToleranceValue(315.0, 400.0, 40.0, 4.0),
                    ToleranceValue(400.0, 500.0, 45.0, 5.0)
                )
            ),
            IsoTolerance(
                "h7", false, listOf(
                    ToleranceValue(0.0, 3.0, 0.0, -10.0), ToleranceValue(3.0, 6.0, 0.0, -12.0),
                    ToleranceValue(6.0, 10.0, 0.0, -15.0), ToleranceValue(10.0, 18.0, 0.0, -18.0),
                    ToleranceValue(18.0, 30.0, 0.0, -21.0), ToleranceValue(30.0, 50.0, 0.0, -25.0),
                    ToleranceValue(50.0, 80.0, 0.0, -30.0), ToleranceValue(80.0, 120.0, 0.0, -35.0)
                )
            ),

            // h9: Wałki ciągnione, prefabrykaty
            IsoTolerance(
                "h9", false, listOf(
                    ToleranceValue(0.0, 3.0, 0.0, -25.0), ToleranceValue(3.0, 6.0, 0.0, -30.0),
                    ToleranceValue(6.0, 10.0, 0.0, -36.0), ToleranceValue(10.0, 18.0, 0.0, -43.0),
                    ToleranceValue(18.0, 30.0, 0.0, -52.0), ToleranceValue(30.0, 50.0, 0.0, -62.0)
                )
            ),

            // p6: Pasowanie ciasne wałka (pod montaż łożysk na wałach)
            IsoTolerance(
                "p6", false, listOf(
                    ToleranceValue(0.0, 3.0, 12.0, 6.0), ToleranceValue(3.0, 6.0, 20.0, 12.0),
                    ToleranceValue(6.0, 10.0, 24.0, 15.0), ToleranceValue(10.0, 18.0, 29.0, 18.0),
                    ToleranceValue(18.0, 30.0, 35.0, 22.0), ToleranceValue(30.0, 50.0, 42.0, 26.0),
                    ToleranceValue(50.0, 80.0, 51.0, 32.0), ToleranceValue(80.0, 120.0, 59.0, 37.0)
                )
            ),

            // d9: Pasowanie bardzo luźne (np. osie kół w maszynach rolniczych)
            IsoTolerance(
                "d9", false, listOf(
                    ToleranceValue(0.0, 3.0, -20.0, -45.0),
                    ToleranceValue(3.0, 6.0, -30.0, -60.0),
                    ToleranceValue(6.0, 10.0, -40.0, -76.0),
                    ToleranceValue(10.0, 18.0, -50.0, -93.0),
                    ToleranceValue(18.0, 30.0, -65.0, -117.0),
                    ToleranceValue(30.0, 50.0, -80.0, -142.0)
                )
            )

        )
    }
}