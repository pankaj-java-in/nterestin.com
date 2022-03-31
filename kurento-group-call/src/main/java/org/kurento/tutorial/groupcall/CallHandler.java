package org.kurento.tutorial.groupcall;

import java.io.IOException;

import org.kurento.client.IceCandidate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public class CallHandler extends TextWebSocketHandler {

  private static final Logger log = LoggerFactory.getLogger(CallHandler.class);

  private static final Gson gson = new GsonBuilder().create();

  @Autowired
  private RoomManager roomManager;

  @Autowired
  private UserRegistry registry;


  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
try {
  final JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

  final UserSession user = registry.getBySession(session);

  if (user != null) {
    log.debug("Incoming message from user '{}': {}", user.getName(), jsonMessage);
  } else {
    log.debug("Incoming message from new user: {}", jsonMessage);
  }

  switch (jsonMessage.get("id").getAsString()) {
    case "joinRoom":
      joinRoom(jsonMessage, session);
      break;
    case "checkRoom":
      checkRoom(jsonMessage, session);
      break;
    case "groupRooms":
      checkGroupRoom(jsonMessage, session);
      break;
    case "receiveVideoFrom":
      final String senderName = jsonMessage.get("sender").getAsString();
      final UserSession sender = registry.getByName(senderName);
      final String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      user.receiveVideoFrom(sender, sdpOffer);
      break;
    case "leaveRoom":
      leaveRoom(user);
      break;
    case "onIceCandidate":
      JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

      if (user != null) {
        IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
                candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
        user.addCandidate(cand, jsonMessage.get("name").getAsString());
      }
      break;
    default:
      break;
  }
} catch (Exception ex) {
  ex.printStackTrace();
}
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    UserSession user = registry.removeBySession(session);
    if (user!=null) {
      roomManager.getRoom(user.getRoomName()).leave(user);
    }
  }

  private void joinRoom(JsonObject params, WebSocketSession session) throws IOException {
    final String roomName = params.get("room").getAsString();
    final String name = params.get("name").getAsString();
    String slotid = params.get("slotid").getAsString();

    final Boolean isPresenter = Boolean.valueOf(params.get("type").getAsString());
    log.info("PARTICIPANT {}: trying to join room {}", name, roomName);
    boolean create = !roomManager.hasRoom(roomName);
    Room room = roomManager.getRoom(roomName);
    if (create && isPresenter) {
      room.sendNewRoom(session);
    }
    if(room.getParticipants().size() < 6) {
      final UserSession user = room.join(name, slotid, session, isPresenter);
      registry.register(user);
    }
  }

  private void checkRoom(JsonObject params, WebSocketSession session) throws IOException {
    final String roomName = params.get("room").getAsString();
    final String name = params.get("name").getAsString();

    log.info("PARTICIPANT {}: trying to join room {}", name, roomName);
    log.info("Has room {} {}", roomName, roomManager.hasRoom(roomName));
    if (roomManager.hasRoom(roomName)) {
      Room room = roomManager.getRoom(roomName);


      room.sendCheckRoom(session);
    } else {
      final JsonObject existingParticipantsMsg = new JsonObject();
      existingParticipantsMsg.addProperty("id", "checkRoom");
      existingParticipantsMsg.add("data", new JsonArray());

      synchronized (session) {
        session.sendMessage(new TextMessage(existingParticipantsMsg.toString()));
      }
    }
  }

  private void checkGroupRoom(JsonObject params, WebSocketSession session) throws IOException {
    final String roomPrefix = params.get("roomPrefix").getAsString();


    log.info("trying to get list of group rooms {}", roomPrefix);


    final JsonArray allGroups = new JsonArray();
    for (final Room room : roomManager.getRoomList()) {
      if (room.getName().startsWith(roomPrefix)) {
        final JsonElement participantName = new JsonPrimitive(room.getName());
        allGroups.add(participantName);
      }
    }

    final JsonObject existingParticipantsMsg = new JsonObject();
    existingParticipantsMsg.addProperty("id", "groupNames");
    existingParticipantsMsg.add("data", allGroups);
    session.sendMessage(new TextMessage(existingParticipantsMsg.toString()));
  }

  private void leaveRoom(UserSession user) throws IOException {
    final Room room = roomManager.getRoom(user.getRoomName());
    room.leave(user);
//    if (room.getParticipants().isEmpty()) {
//      roomManager.removeRoom(room);
//    }
  }
}
