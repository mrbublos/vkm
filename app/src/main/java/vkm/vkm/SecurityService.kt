package vkm.vkm

object SecurityService {

    var _user: User? = null


    fun isLoggedIn(): Boolean {
        // TODO read internal storage if creds exist
        return true
    }

    fun logIn(user: User) {
        // TODO store user in internal storage
        _user = user
    }
}