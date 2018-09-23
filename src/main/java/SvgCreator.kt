import com.opencsv.CSVReaderBuilder
import com.singulariti.os.ephemeris.StarPositionCalculator
import com.singulariti.os.ephemeris.domain.Observatory
import com.singulariti.os.ephemeris.domain.Place
import com.singulariti.os.ephemeris.domain.Pole
import com.singulariti.os.ephemeris.domain.Star
import com.singulariti.os.ephemeris.utils.StarCatalog
import java.io.File
import java.io.FileReader
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*
import java.util.stream.Stream
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

    // at least get this absolute magnitude (smaller is brighter)
    val apparentMagnitudeCutOff = 6.5

    // make the svg file
    File("stars.svg").printWriter().use { out ->

        out.println("""<svg width="180" height="180">""")
        out.println("""<circle cx="90" cy="90" r="90" stroke="white" stroke-width="1" fill="black" />""")
//        history.forEach {
//            out.println("${it.key}, ${it.value}")
//        }
        // second attempt
        val calendarAstronomer = CalendarAstronomer(Date(Instant.parse("2018-10-05T10:15:30.00Z").toEpochMilli()))

        var maxAltitude = Double.NEGATIVE_INFINITY
        var minAltitude = Double.POSITIVE_INFINITY

        var maxMagnitude = Double.NEGATIVE_INFINITY
        var minMagnitude = Double.POSITIVE_INFINITY

        // read the hyg database...
        parseHygDB()
                // filter stars visible to the naked eye
                .filter { star ->
                    star.mag <= apparentMagnitudeCutOff
                }
                .forEach { star ->

                    if (star.absmag < minMagnitude)
                        minMagnitude = star.absmag
                    if (star.absmag > minMagnitude)
                        maxMagnitude = star.absmag

                    val star1 = Star(
                            null,
                            null,
                            null,
                            convertDegreesToHoursMinutesSeconds(star.ra),
                            convertDegreesToHoursMinutesSeconds(star.dec),
                            star.mag.toInt().toString(),
                            null,
                            null
                    )

                    // print the star
                    println("it = $star")

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

                        // determine the size of the circle -> depending on the apparent magnitude
                        val circleR = ((star.mag - apparentMagnitudeCutOff) * -1) * 0.05

                        // print it to svg
                        val random = Random()
                        out.println("""<circle cx="${x}" cy="${y}" r="$circleR" fill="white"/>""")
                    }

                    if (altitude < minAltitude)
                        minAltitude = altitude
                    if (altitude > maxAltitude)
                        maxAltitude = altitude

                    // filter the stars on altitude
                }

        println("minAltitude = ${minAltitude}")
        println("maxAltitude = ${maxAltitude}")
        println("minMagnitude = ${minMagnitude}")
        println("maxMagnitude = ${maxMagnitude}")

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
    val time = ZonedDateTime.of(2018, 10, 5, 16, 0, 0, 0, ZoneId.of("UTC")) //Date and time in UTC
    val place = Place(name, latitude, Pole.NORTH, longitude, Pole.EAST, TimeZone.getTimeZone("Asia/Calcutta"), "", "")
    return Observatory(place, time)
}

/**
 * Convert 2.39483 degrees to X:Y:Z, X hours, Y minutes, Z seconds
 */
fun convertDegreesToHoursMinutesSeconds(degrees: Double): String {
    val hours = Math.floor(degrees).toInt()
    val minutesWithRest = (degrees - hours) * 60
    val minutes = Math.floor(minutesWithRest).toInt()
    val seconds = (minutesWithRest - minutes) * 60
    val result = "$hours:$minutes:$seconds"
    return result
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

/**
 * Read the HYG database.
 */
fun parseHygDB(): Stream<Starr> {
    // get the csv reader
    val csvReader = CSVReaderBuilder(FileReader("data/hygdata_v3.csv"))
            .withSkipLines(1)
            .build()

    // stream the lines
    // indices that we're interested in
    val id = 0
    val hip = 1
    val hd = 2
    val hr = 3
    val gl = 4
    val bf = 5
    val proper = 6
    val ra = 7
    val dec = 8
    val dist = 9
    val pmra = 10
    val pmdec = 11
    val rv = 12
    val mag = 13
    val absmag = 14
    val spect = 15
    val ci = 16
    val x = 17
    val y = 18
    val z = 19
    val vx = 20
    val vy = 21
    val vz = 22
    val rarad = 23
    val decrad = 24
    val pmrarad = 25
    val pmdecrad = 26
    val bayer = 27
    val flam = 28
    val con = 29
    val comp = 30
    val comp_primary = 31
    val base = 32
    val lum = 33
    val varrrr = 34
    val var_min = 35

    return csvReader.map {
        //        println("it = ${Arrays.toString(it)}")
        Starr(
                ra = it[ra].toDouble(),
                dec = it[dec].toDouble(),
                mag = it[mag].toDouble(),
                absmag = it[absmag].toDouble()
        )
    }.stream()

}

data class Starr(val ra: Double, val dec: Double, val mag: Double, val absmag: Double)
