package org.qo.qPluginVelocity;

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.plugin.Plugin
import org.slf4j.Logger

@Plugin(
    id = "qplugin-velocity", name = "QPlugin-Velocity", version = BuildConstants.VERSION
)
class QPluginVelocity @Inject constructor(val logger: Logger) {

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
    }
}
