package org.kurento.tutorial.groupcall;

import org.kurento.client.KurentoClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@SpringBootApplication
@EnableWebSocket
public class GroupCallApp implements WebSocketConfigurer {

  @Bean
  public UserRegistry registry() {
    return new UserRegistry();
  }

  @Bean
  public RoomManager roomManager() {
    return new RoomManager();
  }

  @Bean
  public CallHandler groupCallHandler() {
    return new CallHandler();
  }

  @Bean
  public KurentoClient kurentoClient() {
    return KurentoClient.create();
  }

  public static void main(String[] args) throws Exception {
    SpringApplication.run(GroupCallApp.class, args);
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {

    registry.addHandler(groupCallHandler(), "/groupcall").setAllowedOrigins("*");
  }
}
