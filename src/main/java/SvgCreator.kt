import model.HygParser
import java.io.File
import java.io.PrintWriter
import kotlin.math.*

/**
 * This function parses the input file (input/hygdata_v3.csv) and outputs an SVG file containing all stars above the given observer.
 *
 * Based this code on explanation found at http://jknight8.tripod.com/CelestialToAzEl.html#the%20source%20code
 */

fun main(args: Array<String>) {
    // example values
    val JD = 2458397.0
    val LAT = 51.027930
    val LON = 3.753585

    // create the file
    SvgCreator().createSVGFile(LAT, LON, JD)
}

/**
 * This class is capable of taking the Hyg database and outputting an SVG files full of stars.
 *
 * @param apparentMagnitudeCutOff at least get this absolute magnitude (smaller is brighter, 6.5 = visible by humans)
 * @param nameOffset how much the name of a star is shifted up/right with regards to the star itself
 */
class SvgCreator(val apparentMagnitudeCutOff: Double = 7.0,
                 val outputFile: File = File("output/stars.svg"),
                 val overwriteOutputFile: Boolean = true,
                 val nameOffset: Double = 0.5,
                 val properNameSize: String = "2px",
                 val properNameFont: String = "Trajan Pro",
                 val properNameItalic: Boolean = false
) {

    /**
     * For the given observer's location (lat/lon), at the given time (jd), create a view of all stars overhead.
     */
    fun createSVGFile(LAT: Double, LON: Double, JD: Double) {

        val D = JD - 2451545.0
        val GMSThours = 18.697374558 + 24.06570982441908 * D
        val GMST = (GMSThours % 24) * 15
        val LMST = GMST + LON

        val colorIndices = listOf(
                -0.33 to "O5",
                -0.17 to "B5",
                0.15 to "A5",
                0.44 to "F5",
                0.68 to "G5",
                1.15 to "K5",
                1.64 to "M5"
        )

        // set up the print operation...
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

            // this outputs some default styling... changes this if you want
            out.println("""<svg width="180" height="180">
                        <style>
                            .properName { font: ${if(properNameItalic)"italic" else "normal"} $properNameSize "$properNameFont"; stroke-width: 0.02px; stroke: black; }
                            .constellationName { font: italic 0.2px sans-serif; stroke-width: 0.03px }
                            .colorClassO5 { fill: #9bb0ff }
                            .colorClassB5 { fill: #aabfff }
                            .colorClassA5 { fill: #cad7ff }
                            .colorClassF5 { fill: #f8f7ff }
                            .colorClassG5 { fill: #fff4ea }
                            .colorClassK5 { fill: #ffd2a1 }
                            .colorClassM5 { fill: #ffcc6f }
                            .colorClassDefault { fill: white }
                        </style>
                        <circle cx="90" cy="90" r="90" stroke="black" stroke-width="1" fill="black" />""".trimIndent())

            // loop over stars
            HygParser().parse()
                    .filter { star -> star.mag <= apparentMagnitudeCutOff }
                    .forEach { star ->
                        // take a right ascension in hours, convert to degrees
                        var RA = star.ra
                        RA = (RA % 24) * 15

                        // DEC is in range of -90 to 90... convert to 0 to 360
                        var DEC = star.dec
                        if (DEC < 0)
                            DEC += 360

                        // convert to azimuth / altitude, all in degrees
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

                            // figure out the closest color index that we know
//                    -0.33	 	O5	Blue
//                    -0.17	 	B5	Blue-white
//                    0.15	 	A5	White with bluish tinge
//                    0.44	 	F5	Yellow-White
//                    0.68	 	G5	Yellow
//                    1.15	 	K5	Orange
//                    1.64	 	M5	Red

                            // figure out the color
                            val colorClass = if (star.colorIndex.isNotEmpty()) {

                                val theColor = colorIndices.map { color ->
                                    color to abs(color.first - star.colorIndex.toDouble())
                                }.sortedBy {
                                    it.second
                                }.first().first.second

                                "colorClass$theColor"
                            } else {
                                "colorClassDefault"
                            }

                            // determine the size of the circle -> depending on the apparent magnitude
                            val circleR = ((star.mag - apparentMagnitudeCutOff) * -1) * 0.05

                            // print name?
                            if (star.properName?.isNotEmpty() == true) {
//                    out.println("""<text x="$x" y="$y" class="properName">${star.properName}</text>""")
                                // write the text to the top right of the star
                                out.println("""<text x="${x + nameOffset}" y="${y - nameOffset}" class="properName $colorClass">${star.properName}</text>""")
                            }

                            out.println("""<circle cx="${x}" cy="${y}" r="$circleR" class="$colorClass"/>""")

                            // not really doing anything with this yet... printing the constellation name in a specific color if you want
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

        // ... and actually do the print
        if (overwriteOutputFile) {
            outputFile.delete()
        }
        outputFile.printWriter().use(block)

        println("Finished creating file, you can find it here: ${outputFile.absolutePath}")
    }

}

/**
 * The non-radial version of sin.
 */
fun sinDeg(degrees: Double): Double {
    return sin(Math.toRadians(degrees))
}

/**
 * The non-radial version of cos.
 */
fun cosDeg(degrees: Double): Double {
    return cos(Math.toRadians(degrees))
}

/**
 * The non-radial version of asin.
 */
fun asinDeg(sin: Double): Double {
    return Math.toDegrees(asin(sin))
}

/**
 * The non-radial version of acos.
 */
fun acosDeg(cos: Double): Double {
    return Math.toDegrees(acos(cos))
}
