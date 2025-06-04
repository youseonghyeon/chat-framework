package io.github.youseonghyeon.engine;

import io.github.youseonghyeon.broadcast.MessageBroadcaster;
import io.github.youseonghyeon.engine.config.MessageWriter;
import io.github.youseonghyeon.engine.config.SendFilterPolicy;
import io.github.youseonghyeon.session.SessionStore;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.*;

class ChatManagerTest {

    @Test
    void testSend_BroadcastCalled() {
        SessionStore sessionStore = mock(SessionStore.class);
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        SendFilterPolicy filterPolicy = mock(SendFilterPolicy.class);
        MessageBroadcaster broadcaster = mock(MessageBroadcaster.class);
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run(); // 즉시 실행
            return null;
        }).when(executor).submit(any(Runnable.class));
        Socket self = mock(Socket.class);
        Long roomId = 1L;
        String message = "test-message";
        MessageWriter<String> messageWriter = mock(MessageWriter.class);

        ChatManager chatManager = new ChatManager(sessionStore, executor, filterPolicy, broadcaster);

        when(sessionStore.findRoomBy(roomId)).thenReturn(Set.of());

        chatManager.send(self, roomId, message, messageWriter);

        verify(broadcaster).broadcast(roomId, message);
        verify(sessionStore).findRoomBy(roomId);
        verifyNoInteractions(messageWriter);
    }

}
