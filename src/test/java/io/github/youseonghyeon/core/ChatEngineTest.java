package io.github.youseonghyeon.core;

import io.github.youseonghyeon.broadcast.no.NoOpsBroadcaster;
import io.github.youseonghyeon.config.ChatEngineConfig;
import io.github.youseonghyeon.config.SendFilterPolicy;
import io.github.youseonghyeon.config.adapter.sample.DefaultMessageReceiver;
import io.github.youseonghyeon.config.adapter.sample.DefaultMessageSender;
import io.github.youseonghyeon.core.event.EventType;
import io.github.youseonghyeon.core.event.MessageSubscriber;
import io.github.youseonghyeon.core.event.command.EnterRoom;
import io.github.youseonghyeon.core.event.command.LeaveRoom;
import io.github.youseonghyeon.core.event.command.SendMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ChatEngineTest {

    /**
     * Tests for the initializeDefaultConfiguration method in the ChatEngine class.
     * This method ensures the default configuration of the ChatEngine is correctly initialized.
     */

    @Test
    @DisplayName("기본 설정 초기화 테스트")
    public void testInitializeDefaultConfigurationWhenSendFilterPolicyIsNull() {
        ChatEngineConfig mockConfig = mock(ChatEngineConfig.class);
        when(mockConfig.getSendFilterPolicy()).thenReturn(null);

        ChatEngine chatEngine = new ChatEngine();
        chatEngine.applyConfiguration(config -> mockConfig);

        chatEngine.initializeDefaultConfiguration();

        verify(mockConfig).sendFilterPolicy(isA(SendFilterPolicy.BroadcastExceptSelf.class));
    }

    @Test
    @DisplayName("기본 설정 초기화 테스트 - 메시지 수신기와 송신기가 null인 경우")
    public void testInitializeDefaultConfigurationWhenMessageReceiverAndSenderAreNull() {
        ChatEngineConfig mockConfig = mock(ChatEngineConfig.class);
        when(mockConfig.getMessageReceiver()).thenReturn(null);
        when(mockConfig.getMessageSender()).thenReturn(null);

        ChatEngine chatEngine = new ChatEngine();
        chatEngine.applyConfiguration(config -> mockConfig);

        chatEngine.initializeDefaultConfiguration();

        verify(mockConfig).messageReceiver(isA(DefaultMessageReceiver.class));
        verify(mockConfig).messageSender(isA(DefaultMessageSender.class));
    }

    @Test
    @DisplayName("기본 설정 초기화 테스트 - 메시지 브로드캐스터가 null인 경우")
    public void testInitializeDefaultConfigurationForMessageSubscriberInitialization() {
        ChatEngineConfig mockConfig = mock(ChatEngineConfig.class);
        Map<EventType, MessageSubscriber> messageSubscriberMap = new ConcurrentHashMap<>();
        when(mockConfig.getMessageSubscriberMap()).thenReturn(messageSubscriberMap);

        ChatEngine chatEngine = new ChatEngine();
        chatEngine.applyConfiguration(config -> mockConfig);

        chatEngine.initializeDefaultConfiguration();

        assertTrue(messageSubscriberMap.containsKey(EventType.ENTER));
        assertTrue(messageSubscriberMap.containsKey(EventType.LEAVE));
        assertTrue(messageSubscriberMap.containsKey(EventType.USER_SEND));
        assertTrue(messageSubscriberMap.get(EventType.ENTER) instanceof EnterRoom);
        assertTrue(messageSubscriberMap.get(EventType.LEAVE) instanceof LeaveRoom);
        assertTrue(messageSubscriberMap.get(EventType.USER_SEND) instanceof SendMessage);
    }

    @Test
    @DisplayName("기본 설정 초기화 테스트 - 메시지 브로드캐스터가 null인 경우")
    public void testInitializeDefaultConfigurationWhenMessageBroadcasterIsNull() {
        ChatEngineConfig mockConfig = mock(ChatEngineConfig.class);
        when(mockConfig.getMessageBroadCaster()).thenReturn(null);

        ChatEngine chatEngine = new ChatEngine();
        chatEngine.applyConfiguration(config -> mockConfig);

        chatEngine.initializeDefaultConfiguration();

        verify(mockConfig).messageBroadCaster(isA(NoOpsBroadcaster.class));
    }
}
