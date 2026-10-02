package com.kahootp46.live.websocket.relay;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration(proxyBeanMethods = false)
class RedisRelayConfig {

    @Bean
    RedisMessageListenerContainer liveEventsListenerContainer(RedisConnectionFactory connectionFactory,
            RedisLiveEventSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, new ChannelTopic(LiveEventRelay.CHANNEL));
        return container;
    }
}
