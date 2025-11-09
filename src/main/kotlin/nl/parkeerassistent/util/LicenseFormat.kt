package nl.parkeerassistent.util

enum class LicenseFormat {
    TwoTwoTwo {
        override fun matches(license: String): Boolean {
            return license.length == 6 && isSame(license[0], license[1]) && isSame(license[2], license[3]) && isSame(license[4], license[5])
        }
        override fun format(license: String): String {
            return license.substring(0,2) + "-" + license.substring(2,4) + "-" + license.substring(4)
        }
    },
    OneThreeTwo {
        override fun matches(license: String): Boolean {
            return license.length == 6 && isSame(license[1], license[2], license[3]) && isSame(license[4], license[5])
        }
        override fun format(license: String): String {
            return license.substring(0,1) + "-" + license.substring(1,4) + "-" + license.substring(4)
        }
    },
    TwoThreeOne {
        override fun matches(license: String): Boolean {
            return license.length == 6 && isSame(license[0], license[1]) && isSame(license[2], license[3], license[4])
        }
        override fun format(license: String): String {
            return license.substring(0,2) + "-" + license.substring(2,5) + "-" + license.substring(5)
        }
    },
    ThreeTwoOne {
        override fun matches(license: String): Boolean {
            return license.length == 6 && isSame(license[0], license[1], license[2]) && isSame(license[3], license[4])
        }
        override fun format(license: String): String {
            return license.substring(0,3) + "-" + license.substring(3,5) + "-" + license.substring(5)
        }
    },
    OneTwoThree {
        override fun matches(license: String): Boolean {
            return license.length == 6 && isSame(license[1], license[2]) && isSame(license[3], license[4], license[5])
        }
        override fun format(license: String): String {
            return license.substring(0,1) + "-" + license.substring(1,3) + "-" + license.substring(3)
        }
    },
    ;

    abstract fun matches(license: String): Boolean
    abstract fun format(license: String): String

    companion object Companion {
        fun isSame(char: Char, vararg chars: Char): Boolean {
            var same = true
            for (c in chars) {
                same = same and isSame(char, c)
            }
            return same
        }
        fun isSame(charA: Char, charB: Char): Boolean {
            return ! (isDigit(charA) xor isDigit(charB))
        }
        private fun isDigit(char: Char): Boolean {
            return char in '0'..'9'
        }
    }

}