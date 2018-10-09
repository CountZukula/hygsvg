package model

import com.opencsv.CSVReaderBuilder
import java.io.File
import java.io.FileReader
import java.util.stream.Stream

class HygParser {
    /**
     * Read the HYG database. (v3,
     */
    fun parse(): Stream<HygStar> {
        val s = "input/hygdata_v3.csv"
        // check if the file exists
        val file = File(s)
        if(!file.exists()) {
            throw Error("You have to download 'hygdata_v3.csv' from https://github.com/astronexus/HYG-Database and put it in the input folder in order to use the HygParser class.")
        }

        // get the csv reader
        val csvReader = CSVReaderBuilder(FileReader(file))
                .withSkipLines(1)
                .build()

        // stream the lines
        // indices that we're interested in (check the documentation on astronexus' github)
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

        // convert the relevant information into a HygStar object
        return csvReader.map {
            HygStar(
                    ra = it[ra].toDouble(),
                    dec = it[dec].toDouble(),
                    mag = it[mag].toDouble(),
                    absmag = it[absmag].toDouble(),
                    properName = it[proper],
                    colorIndex = it[ci],
                    bayerFlamsteed = it[bf],
                    constellationAbbreviation = it[con]
            )
        }.stream()

    }
}