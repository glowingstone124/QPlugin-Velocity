package org.qo.qPluginVelocity

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType

object NetworkUtils {
	val client = HttpClient(CIO) {
		engine {
			requestTimeout = 800
		}

		install(io.ktor.client.plugins.HttpTimeout) {
			connectTimeoutMillis = 800
			socketTimeoutMillis = 800
			requestTimeoutMillis = 800
		}

		expectSuccess = false
	}

	suspend fun checkUrl(url: String): Boolean =
		try {
			client.get(url) == HttpStatusCode.OK
		} catch (_: Exception) {
			false
		}

	suspend fun get(
		url: String,
		header: Map<String, String> = emptyMap()
	): String =
		client.get(url) {
			header.forEach { (k, v) -> headers.append(k, v) }
		}.bodyAsText()

	suspend fun post(
		url: String,
		body: Any,
		header: Map<String, String> = emptyMap()
	): String =
		client.post(url) {
			contentType(io.ktor.http.ContentType.Application.Json)
			setBody(body)
			header.forEach { (k, v) -> headers.append(k, v) }
		}.bodyAsText()

	fun close() {
		client.close()
	}
}