package no.nav.emottak.cpa.nfs

import no.nav.emottak.cpa.getEnvVar

class NFSConfig {

    val nfsKey = getEnvVar("CPA_NFS_PRIVATEKEY", "notavailable")
}
