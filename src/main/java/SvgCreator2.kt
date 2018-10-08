import java.io.File
import java.io.PrintWriter
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin


/**
 * http://jknight8.tripod.com/CelestialToAzEl.html#the%20source%20code
 */
fun main(args: Array<String>) {
    // input
    // julian day
    val JD = 2458397
    val LAT = 51.027930
    val LON = 3.753585

    // calculation
    val D = JD - 2451545.0
    val GMSThours = 18.697374558 + 24.06570982441908 * D
    val GMST = (GMSThours % 24) * 15
    val LMST = GMST + LON


    // at least get this absolute magnitude (smaller is brighter) 6.5 = human vis
    val apparentMagnitudeCutOff = 7


    val block: (PrintWriter) -> Unit = { out ->
        /**
         *
        .colorClassO5V { fill: #f0f8ff }
        .colorClassB0V { fill: #f2f6ff }
        .colorClassA0V { fill: #effbff }
        .colorClassF0V { fill: #fffffb }
        .colorClassG0V { fill: #ffffce }
        .colorClassK0V { fill: #fff8a0 }
        .colorClassM0V { fill: #fff8a0 }
        .colorClassDefault { fill: white }


        http://www.vendian.org/mncharity/dir3/starcolor/
         */

        out.println("""<svg width="180" height="180">
                        <style>
                            .properName { font: italic 1px sans-serif; stroke-width: 0.02px; stroke: black; }
                            .constellationName { font: italic 0.2px sans-serif; stroke-width: 0.03px }
                            .colorClassO5V { fill: #9bb0ff }
                            .colorClassB0V { fill: #aabfff }
                            .colorClassA0V { fill: #cad7ff }
                            .colorClassF0V { fill: #f8f7ff }
                            .colorClassG0V { fill: #fff4ea }
                            .colorClassK0V { fill: #ffd2a1 }
                            .colorClassM0V { fill: #ffcc6f }
                            .colorClassDefault { fill: white }
                        </style>
                        <circle cx="90" cy="90" r="90" stroke="white" stroke-width="1" fill="black" />""".trimIndent())

        // loop over stars
        HygParser().parse().forEach { star ->
            var RA = star.ra

            // 26 % 24 = 2
            // 2 % 24 = 2
            // 27 % 24 = 3
            // 102 % 10 = 2

            RA = (RA % 24) * 15

            var DEC = star.dec
            // DEC is in range of -90 to 90... convert to 0 to 360
            if (DEC < 0)
                DEC += 360


            // convert to az / alt
            var HA = LMST - RA
            if (HA < 0)
                HA += 360

            val sinALT = (sinDeg(DEC) * sinDeg(LAT)) + (cosDeg(DEC) * cosDeg(LAT) * cosDeg(HA))
            val ALT = asinDeg(sinALT)
            val cosA = (sinDeg(DEC) - sinDeg(ALT) * sinDeg(LAT)) / (cosDeg(ALT) * cosDeg(LAT))
            val A = acosDeg(cosA)
            val AZ =
                    if (sinDeg(HA) < 0) {
                        A
                    } else {
                        360 - A
                    }

            println("LAT = $LAT RA = $RA DEC = $DEC SINALT = $sinALT  ALT = $ALT HA = $HA AZ = $AZ")

            if (ALT < 0) {
                // do nothing... it's below to horizon
            } else {
                // altitude 0 means the outside of the circle... r is 90 then
                val r = 90 - ALT
                // draw the star! figure out an x,y coordinate on a circle
                var y = sinDeg(AZ) * r
                var x = cosDeg(AZ) * r


                // shift everything +90 -> make sure the center of the circle is at (90,90)
                y += 90
                x += 90

                // figure out the color
                val colorClass = if (star.colorIndex.isNotEmpty()) {
                    star.colorIndex.toDouble().let {
                        when {
                            it <= -.33 -> "colorClassO5V"
                            it <= -.3 -> "colorClassB0V"
                            it <= -0.02 -> "colorClassA0V"
                            it <= 0.3 -> "colorClassF0V"
                            it <= 0.58 -> "colorClassG0V"
                            it <= 0.81 -> "colorClassK0V"
                            it <= 1.40 -> "colorClassM0V"
                            else -> "colorClassDefault"
                        }
                    }
                } else {
                    "colorClassDefault"
                }

                // determine the size of the circle -> depending on the apparent magnitude
                val circleR = ((star.mag - apparentMagnitudeCutOff) * -1) * 0.05

                // print name?
                val nameOffset = 0.5
                if (star.properName?.isNotEmpty() == true) {
//                    out.println("""<text x="$x" y="$y" class="properName">${star.properName}</text>""")
                    // write the text to the top right of the star
                    out.println("""<text x="${x+nameOffset}" y="${y-nameOffset}" class="properName $colorClass">${star.properName}</text>""")
                }

                println("circleR = ${circleR}")

                if(circleR>0) {
                    out.println("""<circle cx="${x}" cy="${y}" r="$circleR" class="$colorClass"/>""")
                }

//                if (star.constellationAbbreviation?.isNotEmpty()) {
//                    val col = when (star.constellationAbbreviation) {
//                        "Dra" -> "red"
//                        "UMa" -> "green"
//                        "Her" -> "blue"
//                        else -> "white"
//                    }
//                    out.println("""<text x="$x" y="$y" class="constellationName" fill="$col" stroke="$col">${star.constellationAbbreviation}</text>""")
//                }
            }
        }

        out.println("</svg>")
    }

    File("stars2.svg").printWriter().use(block)

}

fun sinDeg(degrees: Double): Double {
    return sin(Math.toRadians(degrees))
}

fun cosDeg(degrees: Double): Double {
    return cos(Math.toRadians(degrees))
}

fun asinDeg(sin: Double): Double {
    return Math.toDegrees(asin(sin))
}

fun acosDeg(cos: Double): Double {
    return Math.toDegrees(acos(cos))
}
