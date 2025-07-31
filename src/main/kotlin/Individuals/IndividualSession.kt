package J.dev.Individuals

import kotlinx.serialization.Serializable

@Serializable
data class IndividualSession(val username :String, val count :Int) {

}