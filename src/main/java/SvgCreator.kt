import com.opencsv.CSVReaderBuilder
import com.singulariti.os.ephemeris.StarPositionCalculator
import com.singulariti.os.ephemeris.domain.Observatory
import com.singulariti.os.ephemeris.domain.Place
import com.singulariti.os.ephemeris.domain.Pole
import com.singulariti.os.ephemeris.domain.Star
import com.singulariti.os.ephemeris.utils.StarCatalog
import java.io.File
import java.io.FileReader
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream
import javax.xml.datatype.DatatypeConstants.HOURS
import kotlin.math.absoluteValue


/**
 * <svg width="100" height="100">
<circle cx="50" cy="50" r="40" stroke="green" stroke-width="4" fill="yellow" />
</svg>

180 becomes 18000 -> add 2 digits of precision to everything
 */
fun main(args: Array<String>) {
    // set the place and time you want
    val starPositionCalculator = StarPositionCalculator()

    val observatory = getObservatory()

    // at least get this absolute magnitude (smaller is brighter) 6.5 = human vis
    val apparentMagnitudeCutOff = 7

    // make the svg file
    File("stars.svg").printWriter().use { out ->

        out.println(""" <svg width="180" height="180">
                        <style>
                            .properName { font: italic 1px sans-serif; stroke-width: 0.03px; stroke: white; fill: black }
                            .constellationName { font: italic 0.2px sans-serif; stroke-width: 0.03px }
                            .colorClassO5V { fill: #f0f8ff }
                            .colorClassB0V { fill: #f2f6ff }
                            .colorClassA0V { fill: #effbff }
                            .colorClassF0V { fill: #fffffb }
                            .colorClassG0V { fill: #ffffce }
                            .colorClassK0V { fill: #fff8a0 }
                            .colorClassDefault { fill: white }
                        </style>
                        <circle cx="90" cy="90" r="90" stroke="white" stroke-width="1" fill="black" />""".trimIndent())
//        history.forEach {
//            out.println("${it.key}, ${it.value}")
//        }
        // second attempt
        val calendarAstronomer = CalendarAstronomer(Date(Instant.parse("2018-10-05T10:15:30.00Z").toEpochMilli()))

        val colorIndices = HashSet<String>()

        val casA = StarCatalog.byIdAndConstellation("a", "cas")

        // read the hyg database...
        HygParser()
                .parse()
                // filter stars visible to the naked eye
                .filter { star ->
                    star.mag <= apparentMagnitudeCutOff
                }
                .forEach { star ->

                    colorIndices.add(star.colorIndex)

                    val convertDegreesToHoursMinutesSeconds = convertDecimalHoursToHMS(star.ra)
                    val convertDegreesToHoursMinutesSeconds1 = convertDecimalHoursToHMS(star.dec)

                    println("convertDegreesToHoursMinutesSeconds = ${convertDegreesToHoursMinutesSeconds} ${convertDegreesToHoursMinutesSeconds1}")

                    val star1 = Star(
                            null,
                            null,
                            null,
                            convertDegreesToHoursMinutesSeconds,
                            convertDegreesToHoursMinutesSeconds1,
                            star.mag.toInt().toString(),
                            null,
                            null
                    )

                    // print the star
                    println("it = $star")
                    println(" > star1 = ${star1.ra} ${star1.de}")

                    val position = starPositionCalculator.getPosition(star1, observatory)
//        println("position = $position")

                    // is the star inside the radius?
//        getDistanceFromLatLonInKm(observatory.latitude, observatory.longitude, position.);

//            println("position.altitude = ${position.altitude}, ${position.azimuth} ${dmsToRad(position.altitude)}")


                    val altitude = dmsToRad(position.altitude)

                    assert(altitude.absoluteValue <= 90)

                    // only allow stars above the horizon
                    if (altitude >= 0) {
                        // alpha -> the angle on the circle, which would be the azimuth
                        val azimuth = dmsToRad(position.azimuth)
                        // r -> this represents the altitude, map it to 0-90
                        // altitude 0 means the outside of the circle... r is 90 then
                        val r = 90 - altitude
                        // draw the star! figure out an x,y coordinate on a circle
                        var y = Math.sin(azimuth) * r
                        var x = Math.cos(azimuth) * r

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

                        // print it to svg
                        val random = Random()
                        out.println("""<circle cx="${x}" cy="${y}" r="$circleR" class="$colorClass"/>""")

                        // print name?
                        if (star.properName?.isNotEmpty() == true) {
                            out.println("""<text x="$x" y="$y" class="properName">${star.properName}</text>""")
                        }

                        if (star.constellationAbbreviation?.isNotEmpty()) {
                            val col = when (star.constellationAbbreviation) {
                                "Dra" -> "red"
                                "UMa" -> "green"
                                "Her" -> "blue"
                                else -> "white"
                            }
                            out.println("""<text x="$x" y="$y" class="constellationName" fill="$col" stroke="$col">${star.constellationAbbreviation}</text>""")
                        }
                    } else {
//                        println("skipping star... $altitude")
                    }

                    // filter the stars on altitude
                }

        val minRaa = HygParser().parse().mapToDouble { star -> star.ra }.min()
        println("minRa = ${minRaa}")
        val maxRaa = HygParser().parse().mapToDouble { star -> star.ra }.max()
        println("maxRa = ${maxRaa}")
        val minDec = HygParser().parse().mapToDouble { star -> star.dec }.min()
        println("minDec = ${minDec}")
        val maxDec = HygParser().parse().mapToDouble { star -> star.dec }.max()
        println("maxDec = ${maxDec}")

        out.println("</svg>")
    }

}

/**
 * Convert XX:XX to degrees.
 */
fun dmsToRad(input: String): Double {
    val resMod = if (input.startsWith("-")) -1 else 1
    val split = input.split(":")
    var result = 0.0
    result += split[0].toDouble().absoluteValue
    if (split.size > 1)
        result += (split[1].toDouble() / 60)
    return result * resMod
}

/**
 * Default position: gontrodestraat
 */
fun getObservatory(name: String = "Default place name", latitude: Double = 51.027930, longitude: Double = 3.753585): Observatory {
    val time = ZonedDateTime.of(2018, 10, 5, 19, 0, 0, 0, ZoneId.of("UTC")) //Date and time in UTC
    val place = Place(name, latitude, Pole.NORTH, longitude, Pole.EAST, TimeZone.getTimeZone("Asia/Calcutta"), "", "")
    return Observatory(place, time)
}

/**
 * Convert 21.9384 hours to X:Y:Z, X hours, Y minutes, Z seconds
 */
fun convertDecimalHoursToHMS(degrees: Double): String {
    // what's the sign?
    val sign = if(degrees<0) "-" else "+"
    val input = degrees.absoluteValue
    val hours = Math.floor(input).toInt()
    val minutesWithRest = (input - hours) * 60
    val minutes = Math.floor(minutesWithRest).toInt()
    val seconds = ((minutesWithRest - minutes) * 60).toInt()
    val result = "${sign}$hours:$minutes:$seconds"
    return result
}

/**
 * Convert 25.5 degrees to 25 degrees, 5
 */
fun convertDecimalDegreesToDMS(degrees: Double): String {
    return convertDecimalHoursToHMS(degrees)
}

fun testEphemeris() {


    val starCalculator = StarPositionCalculator()
    val casA = StarCatalog.byIdAndConstellation("a", "cas")
//    val casAPosition = starCalculator.getPosition(casA, hassan)

//    println("casA = $casAPosition")
}

fun getDistanceFromLatLonInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    var R = 6371; // Radius of the earth in km
    var dLat = deg2rad(lat2 - lat1);  // deg2rad below
    var dLon = deg2rad(lon2 - lon1);
    var a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)
    ;
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    var d = R * c; // Distance in km
    return d
}

fun deg2rad(deg: Double): Double {
    return deg * (Math.PI / 180)
}

fun hoursToDms(hours: Double) {

}
