package io.github.youseonghyeon.session;

import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SessionStoreTest {

    /**
     * Tests for registerSocketToRoom method in the InMemorySessionStore class.
     * <p>
     * The registerSocketToRoom method is responsible for registering a Socket instance to a specified room.
     * Internally, it utilizes helper methods to handle updates to both the primary store and an inverted index of sockets and rooms.
     */

    @Test
    void testRegisterSocketToEmptyRoom() {
        // Arrange
        SessionStore store = new SessionStore();
        Socket socket = new Socket();
        Long roomId = 1L;

        // Act
        store.registerSocketToRoom(socket, roomId);

        // Assert
        Set<Socket> socketsInRoom = store.findRoomBy(roomId);
        assertNotNull(socketsInRoom);
        assertTrue(socketsInRoom.contains(socket));
    }

    @Test
    void testRegisterSocketToNullRoom() {
        // Arrange
        SessionStore store = new SessionStore();
        Socket socket = new Socket();
        Long roomId = null;

        // Act & Assert
        assertThrows(NullPointerException.class, () -> store.registerSocketToRoom(socket, roomId));
    }

    @Test
    void testRegisterSocketToRoomConcurrentAccess() {
        // Arrange
        SessionStore store = new SessionStore();
        Socket socket1 = new Socket();
        Socket socket2 = new Socket();
        Long roomId = 7L;

        // Act
        Thread thread1 = new Thread(() -> store.registerSocketToRoom(socket1, roomId));
        Thread thread2 = new Thread(() -> store.registerSocketToRoom(socket2, roomId));
        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            fail("Thread execution interrupted");
        }

        // Assert
        Set<Socket> socketsInRoom = store.findRoomBy(roomId);
        assertNotNull(socketsInRoom);
        assertEquals(2, socketsInRoom.size());
        assertTrue(socketsInRoom.contains(socket1));
        assertTrue(socketsInRoom.contains(socket2));
    }

    @Test
    void testRegisterSameSocketToDifferentRoomsInReverseLookup() {
        // Arrange
        SessionStore store = new SessionStore().enableReverseLookup();
        Socket socket = new Socket();
        Long roomId1 = 8L;
        Long roomId2 = 9L;

        // Act
        store.registerSocketToRoom(socket, roomId1);
        store.registerSocketToRoom(socket, roomId2);

        // Assert
        Set<Socket> socketsInRoom1 = store.findRoomBy(roomId1);
        Set<Socket> socketsInRoom2 = store.findRoomBy(roomId2);

        assertNotNull(socketsInRoom1);
        assertNotNull(socketsInRoom2);
        assertTrue(socketsInRoom1.contains(socket));
        assertTrue(socketsInRoom2.contains(socket));
    }

    @Test
    void testRegisterSocketToNonEmptyRoom() {
        // Arrange
        SessionStore store = new SessionStore();
        Socket socket1 = new Socket();
        Socket socket2 = new Socket();
        Long roomId = 2L;

        store.registerSocketToRoom(socket1, roomId);

        // Act
        store.registerSocketToRoom(socket2, roomId);

        // Assert
        Set<Socket> socketsInRoom = store.findRoomBy(roomId);
        assertNotNull(socketsInRoom);
        assertEquals(2, socketsInRoom.size());
        assertTrue(socketsInRoom.contains(socket1));
        assertTrue(socketsInRoom.contains(socket2));
    }

    @Test
    void testRegisterSameSocketToRoomIdempotency() {
        // Arrange
        SessionStore store = new SessionStore();
        Socket socket = new Socket();
        Long roomId = 3L;

        store.registerSocketToRoom(socket, roomId);

        // Act
        store.registerSocketToRoom(socket, roomId);

        // Assert
        Set<Socket> socketsInRoom = store.findRoomBy(roomId);
        assertNotNull(socketsInRoom);
        assertEquals(1, socketsInRoom.size());
        assertTrue(socketsInRoom.contains(socket));
    }

    @Test
    void testRegisterSocketToMultipleRooms() {
        // Arrange
        SessionStore store = new SessionStore();
        Socket socket = new Socket();
        Long roomId1 = 4L;
        Long roomId2 = 5L;

        // Act
        store.registerSocketToRoom(socket, roomId1);
        store.registerSocketToRoom(socket, roomId2);

        // Assert
        Set<Socket> socketsInRoom1 = store.findRoomBy(roomId1);
        Set<Socket> socketsInRoom2 = store.findRoomBy(roomId2);

        assertNotNull(socketsInRoom1);
        assertNotNull(socketsInRoom2);
        assertTrue(socketsInRoom1.contains(socket));
        assertTrue(socketsInRoom2.contains(socket));
    }
}
