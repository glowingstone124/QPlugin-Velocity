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

class NetworkUtils {
	val client = HttpClient(CIO)
	suspend fun checkUrl(url: String): Boolean {
		 return try {
			 return client.get(url).status == HttpStatusCode.OK
		 } catch (e: Exception) {
			 false
		 }
	}
	suspend fun post(url: String, body: Any,header: Map<String, String> = emptyMap()): String {
		return client.post(url) {
			contentType(io.ktor.http.ContentType.Application.Json)
			setBody(body)
			header.forEach { (key, value) ->
				headers.append(key, value)
			}
		}.bodyAsText()
	}
	suspend fun get(url: String, header: Map<String, String> = emptyMap()): String {
		return client.get(url) {
			header.forEach { (key, value) ->
				headers.append(key, value)
			}
		}.bodyAsText()
	}
}