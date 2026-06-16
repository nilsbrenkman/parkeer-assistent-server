package nl.parkeerassistent.util


object LicenseUtil {

    fun normalise(license: String): String {
        val normalised = license.replace("[^0-9a-zA-Z]".toRegex(), "")
        return normalised.uppercase()
    }

    fun format(license: String) : String {
        val normalised = normalise(license)
        for (format in LicenseFormat.values()) {
            if (format.matches(normalised)) {
                return format.format(normalised)
            }
        }
        return normalised
    }

}

