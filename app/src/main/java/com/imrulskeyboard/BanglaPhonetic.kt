package com.imrulskeyboard

/**
 * Small rule-based phonetic transliterator for the starter app.
 * This is intentionally not advertised as the full Avro Phonetic engine.
 */
object BanglaPhonetic {
    private val words = mapOf(
        "ami" to "আমি", "tumi" to "তুমি", "apni" to "আপনি",
        "bangla" to "বাংলা", "bangladesh" to "বাংলাদেশ",
        "bhalo" to "ভালো", "valo" to "ভালো", "kemon" to "কেমন",
        "acho" to "আছো", "achhi" to "আছি", "hoy" to "হয়",
        "hello" to "হ্যালো", "dhonnobad" to "ধন্যবাদ",
        "amar" to "আমার", "tomar" to "তোমার", "naam" to "নাম",
        "ki" to "কি", "keno" to "কেন", "kothay" to "কোথায়",
        "chai" to "চাই", "korbo" to "করব", "koro" to "করো",
        "likho" to "লিখো", "phone" to "ফোন", "keyboard" to "কিবোর্ড"
    )

    private val replacements = listOf(
        "kh" to "খ", "gh" to "ঘ", "chh" to "ছ", "ch" to "চ",
        "jh" to "ঝ", "th" to "থ", "dh" to "ধ", "ph" to "ফ",
        "bh" to "ভ", "sh" to "শ", "ng" to "ঙ", "ny" to "ঞ",
        "rr" to "ড়", "rh" to "ঢ়", "k" to "ক", "g" to "গ",
        "j" to "জ", "t" to "ত", "d" to "দ", "n" to "ন",
        "p" to "প", "f" to "ফ", "b" to "ব", "m" to "ম",
        "r" to "র", "l" to "ল", "s" to "স", "h" to "হ",
        "y" to "য", "w" to "ও", "v" to "ভ", "z" to "জ",
        "a" to "া", "i" to "ি", "e" to "ে", "o" to "ো",
        "u" to "ু", "c" to "ক", "x" to "ক্স"
    )

    fun convert(input: String): String {
        if (input.isBlank()) return ""
        val key = input.lowercase()
        words[key]?.let { return it }

        // Process longer digraphs first. Vowel signs are used as a simple approximation.
        var out = key
        replacements.sortedByDescending { it.first.length }.forEach { (latin, bengali) ->
            out = out.replace(latin, bengali)
        }
        return out
    }
}
