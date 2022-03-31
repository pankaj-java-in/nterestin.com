/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.groupcall;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.3.1
 */
public class RoomManager {

  private final Logger log = LoggerFactory.getLogger(RoomManager.class);

  @Autowired
  private KurentoClient kurento;

  private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

  public Room getRoom(String roomName) {
    log.debug("Searching for room {}", roomName);
    Room room = rooms.get(roomName);

    if (room == null) {
      log.debug("Room {} not existent. Will create now!", roomName);
      room = new Room(roomName, kurento.createMediaPipeline());
      rooms.put(roomName, room);
    }
    log.debug("Room {} found!", roomName);
    return room;
  }

  public boolean hasRoom(String roomName) {
    log.debug("checking for room {}", roomName);
    return rooms.get(roomName) != null;

  }

  public void healthcheck() {
    for (String roomName:
    rooms.keySet()) {
      rooms.get(roomName).healthcheck();

    }
  }
//
//  public List<Room> getGroupRooms(String roomGroupPrefix) {
//    log.debug("Searching for room {}", roomGroupPrefix);
//    return rooms.values().stream().
//            filter(r -> r.getName().startsWith(roomGroupPrefix)).
//            collect(Collectors.toList());
//  }

  /**
   * Removes a room from the list of available rooms.
   *
   * @param room
   *          the room to be removed
   */
  public void removeRoom(Room room) {
    this.rooms.remove(room.getName());
    room.close();
    log.info("Room {} removed and closed", room.getName());
  }

  public Collection<Room> getRoomList() {
    return rooms.values();
  }

}
