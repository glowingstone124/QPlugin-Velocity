package org.qo.qPluginVelocity;

import com.google.gson.JsonParser
import com.google.inject.Inject
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger
import java.util.concurrent.TimeUnit

@Plugin(
	id = "qplugin-velocity",
	name = "QPlugin-Velocity",
	version = BuildConstants.VERSION
)
class QPluginVelocity @Inject constructor(
	val logger: Logger,
	val server: ProxyServer
) {

	companion object {
		@Volatile
		var loc: String = "UNKNOWN"

		@Volatile
		var backendReady = false

		@Volatile
		var failureCnt = 0
	}

	private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

	@Subscribe
	fun onProxyInitialization(event: ProxyInitializeEvent) {
		logger.info("[QPluginVelocity] Initializing...")
		ioScope.launch {
			initRemoteInfo()
			startHeartbeat()
		}
	}

	private suspend fun initRemoteInfo() {
		try {
			val json = NetworkUtils.get(
				"${Configuration.CONFIG.endpoint}/qo/proxies/query?token=${Configuration.CONFIG.token}",
			)

			val obj = JsonParser.parseString(json).asJsonObject
			if (obj["code"].asInt == 0) {
				loc = obj["name"].asString
				logger.info("Loaded region: $loc, Backend ready")
				backendReady = true
			} else {
				logger.warn("Region not registered")
			}
		} catch (e: Exception) {
			logger.warn("Init remote info failed: ${e.message}")
		}
	}

	@Subscribe
	fun onLogin(event: LoginEvent) {
		if (!backendReady) {
			event.result = ResultedEvent.ComponentResult.denied(
				Component.text("我们这里出了错。").appendNewline()
					.append(Component.text("和QAPI的通讯出现了短暂错误，请稍后再试。"))
					.color(NamedTextColor.YELLOW)
			)
			return
		}
		if (loc == "UNKNOWN") {
			event.result = ResultedEvent.ComponentResult.denied(
				Component.text("我们这里出了错。").appendNewline()
					.append(Component.text("网关无法连接到QAPI，如果该问题持续出现，请联系管理。"))
					.color(NamedTextColor.RED)
			)
		}
	}

	@Subscribe
	fun onProxyPing(event: ProxyPingEvent) {
		val motd = Component.text("Quantum Original")
			.color(NamedTextColor.BLUE)
			.appendNewline()
			.append(
				Component.text("当前节点：$loc")
					.color(NamedTextColor.GRAY)
			)

		event.ping = event.ping.asBuilder()
			.description(motd)
			.build()
	}

	@Subscribe
	fun onProxyShutdown(event: ProxyShutdownEvent) {
		NetworkUtils.close()
	}

	private fun startHeartbeat() {
		server.scheduler.buildTask((this), Runnable {
			ioScope.launch {
				try {
					NetworkUtils.post("${Configuration.CONFIG.endpoint}/qo/proxies/accept", Configuration.CONFIG.token)

					failureCnt = 0
					if (!backendReady) {
						backendReady = true
						logger.info("[QPluginVelocity] Backend ready")
					}
				} catch (e: Exception) {
					failureCnt++
					logger.error("Heartbeat failed: ${e.message}")
					if (failureCnt >= 3 && backendReady) {
						backendReady = false
						logger.info("[QPluginVelocity] Heartbeat failed: ${failureCnt}, backend marked offline.")
					}
				}
			}
		}).repeat(800, TimeUnit.MILLISECONDS)
			.schedule()
	}
}

object BuildConstants {
	const val VERSION = "Alpha 0.2"
}
