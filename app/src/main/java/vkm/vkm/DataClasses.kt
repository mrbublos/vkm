package vkm.vkm

data class Composition(var name: String, var url: String, var artist: String, var progress: Int = 0, var hash: String, var length: String)

data class User(var userId: String, var password: String, var token: String)