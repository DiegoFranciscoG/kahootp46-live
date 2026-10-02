package com.kahootp46.live.websocket.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import com.kahootp46.live.shared.web.CorsProperties;
import com.kahootp46.live.websocket.LiveTopics;

/**
 * STOMP over native WebSocket. Each instance runs an in-memory simple broker; events reach every instance
 * through Redis Pub/Sub (see {@code relay}), so viewers can be connected to any instance.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final long HEARTBEAT_MS = 10_000;

    private final CorsProperties cors;
    private final StompAuthInterceptor authInterceptor;
    private final TaskScheduler brokerScheduler;

    WebSocketConfig(CorsProperties cors, StompAuthInterceptor authInterceptor,
            @Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler brokerScheduler) {
        this.cors = cors;
        this.authInterceptor = authInterceptor;
        this.brokerScheduler = brokerScheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Explicit Origin allow-list: browsers do not apply the same-origin policy to WebSockets (CSWSH).
        registry.addEndpoint("/ws").setAllowedOrigins(cors.allowedOrigins().toArray(String[]::new));
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic")
                .setHeartbeatValue(new long[] {HEARTBEAT_MS, HEARTBEAT_MS})
                .setTaskScheduler(brokerScheduler);
        registry.setApplicationDestinationPrefixes(LiveTopics.APP_PREFIX);
        // Events of a game must reach each viewer in the order they were published.
        registry.setPreservePublishOrder(true);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Clients only send small control frames (CONNECT/SUBSCRIBE).
        registration.setMessageSizeLimit(8 * 1024)
                .setSendBufferSizeLimit(512 * 1024)
                .setSendTimeLimit(15_000);
    }
}
