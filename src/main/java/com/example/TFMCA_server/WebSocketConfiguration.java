package com.example.TFMCA_server;

import org.springframework.context.annotation.Configuration;

import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
class WebSocketConfiguration implements WebSocketConfigurer  {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebSocketHandler(), "/tfmca");
    }
}