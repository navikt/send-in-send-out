package no.nav.emottak

fun removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(xml: String): String {
    return xml.replace(">\\s+".toRegex(), ">")
        .replace("\\s+<".toRegex(), "<")
        .replace("\\s+".toRegex(), " ")
}

fun loggDiff(expectedXml: String, xml: String) {
    val x1 = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(expectedXml)
    val x2 = removeWhitespaceBetweenXmlElementsAndMinimiseOtherWhitespace(xml)
    var i = 0
    var j = 0
    while (i < x1.length && j < x2.length) {
        if (x1[i] != x2[j]) {
            println("Mismatch at index $i: expected '${x1[i]}', got '${x2[j]}'")
        }
        i++
        j++
    }
}
