package no.nav.emottak.frikort

import com.sun.xml.bind.marshaller.NamespacePrefixMapper

class FrikortNamespacePrefixMapper : NamespacePrefixMapper() {
    companion object {
        private val PREFIXES = mapOf(
            "http://www.kith.no/xmlstds/msghead/2006-05-24" to "mh",
            "http://www.w3.org/1999/xlink" to "xlink",
            "http://www.w3.org/2000/09/xmldsig#" to "dsig",
            "http://www.w3.org/2009/xmldsig11#" to "dsig11"
        )
    }

    override fun getPreferredPrefix(namespaceUri: String, suggestion: String?, requirePrefix: Boolean): String? =
        PREFIXES[namespaceUri]
}
