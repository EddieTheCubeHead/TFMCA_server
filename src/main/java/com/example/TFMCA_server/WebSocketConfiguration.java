package com.example.TFMCA_server;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

//https://www.toptal.com/java/stomp-spring-boot-websocket
@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(), "/tfmca");
    }
}