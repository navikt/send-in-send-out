package jakarta.xml.bind

interface Validator {
    // Dummy to make CXF work with its own incompatible dependency to jakarta.xml.bind-api.
    //
    // It seems that all the CXF versions that use jakarta.xml (as opposed to javax.xml)
    // contains some code (for WSDL-to-java) that needs the interface class jakarta.xml.bind.Validator to exist.
    // That class was deprecated in jakarta, and CXF uses newer versions of jakarta which do NOT include it.
    // So, it seems that it is impossible to execute such WSDL-related code like the IT tests in this project do,
    // with jakarta XML.
    // To make the switch to jakarta, we either need to rewrite the tests in some way,
    // or to make a fake jakarta.xml.bind.Validator so CXF is happy.
    // As those tests seem very complex and hard to understand (and rewrite), the latter approach has been taken.
}
