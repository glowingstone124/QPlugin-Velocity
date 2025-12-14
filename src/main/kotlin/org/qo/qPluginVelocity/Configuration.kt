package org.qo.qPluginVelocity

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

object Configuration {
	val path = "config.json"
	var CONFIG: Config = Json.decodeFromString<Config>(File(path).readText())
}

@Serializable
data class Config(
	val endpoint: String,
	val token: String,
	val connectTimeoutMillis: Long,
	val socketTimeMillis: Long,
	val requestTimeoutMillis: Long,
)