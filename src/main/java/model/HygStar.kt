package model

/**
 * This class represents a single line in the Hyg database.
 *
 * @param colorIndex The star's color index (blue magnitude - visual magnitude), where known
 */
data class HygStar(
        val ra: Double,
        val dec: Double,
        val mag: Double,
        val absmag: Double,
        val properName: String?,
        val colorIndex: String,
        val bayerFlamsteed: String,
        val constellationAbbreviation: String
)