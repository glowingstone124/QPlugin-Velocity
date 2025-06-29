package org.qo.qPluginVelocity;

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.quartz.Job
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.quartz.JobBuilder
import org.quartz.JobExecutionContext
import org.quartz.SimpleScheduleBuilder
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.Logger
import java.net.InetAddress

@Plugin(
    id = "qplugin-velocity", name = "QPlugin-Velocity", version = BuildConstants.VERSION
)
class QPluginVelocity @Inject constructor(val logger: Logger, val proxy: ProxyServer){
    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        runBlocking { asyncProxyInitialization(event) }
    }
    suspend fun asyncProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("[QPluginVelocity] Initializing plugin...")
        logger.info("[QPluginVelocity] Testing endpoint: ${Configuration.CONFIG.endpoint}")
        if (!NetworkUtils.checkUrl(Configuration.CONFIG.endpoint)) {
            logger.warn("[QPluginVelocity] Failed to connect to ${Configuration.CONFIG.endpoint}!")
        }
        logger.info("[QPluginVelocity] Starting job...")
        val job = JobBuilder.newJob(HeartbeatJob::class.java).withIdentity("MyJob", "group1").build()
        val trigger = TriggerBuilder.newTrigger().withIdentity("MyTrigger", "group1")
            .startNow().withSchedule(
                SimpleScheduleBuilder.simpleSchedule().withIntervalInMilliseconds(1500)
                    .repeatForever()
            )
            .build()
        val scheduler = StdSchedulerFactory.getDefaultScheduler()
        scheduler.start()
        scheduler.scheduleJob(job, trigger)
    }
    @Subscribe
    fun onProxyPing(event: ProxyPingEvent) = runBlocking {
        val ip: InetAddress = event.connection.remoteAddress.address

        val motd: Component =
            when (NetworkUtils.get("${Configuration.CONFIG.endpoint}/qo/download/ip?ip=${ip.hostAddress}")) {
                "false" -> Component.text("Quantum Original Global").color(NamedTextColor.BLUE).appendNewline()
                    .content("Join our discord -> https://discord.gg/kWfNRNRC").color(NamedTextColor.GREEN)

                "true" -> Component.text("Quantum Original 2").color(NamedTextColor.BLUE).appendNewline()
                    .content("加入我们的qq群：946085440").color(NamedTextColor.GREEN)

                else -> Component.text("Quantum Original Global").color(NamedTextColor.BLUE).appendNewline()
                    .content("Join our discord -> https://discord.gg/kWfNRNRC").color(NamedTextColor.GREEN)
            }
        val pong = event.ping.asBuilder()
        pong.description(motd)
        event.ping = pong.build()
    }

}
class HeartbeatJob : Job {
    override fun execute(ctx: JobExecutionContext) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NetworkUtils.post(Configuration.CONFIG.endpoint + "/qo/proxies/accept", Configuration.CONFIG.token)
            } catch (e: Exception) {
                println("Heartbeat failed: ${e.message}")
            }
        }
    }
}

object BuildConstants {
    const val VERSION = "Alpha 0.1"
}