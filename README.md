# ChattingEngine Framework

<p>
ChattingEngine Framework는 Java 기반의 소켓 통신 환경에서 실시간 채팅 시스템을 구축하기 위한 경량 프레임워크입니다.<br>
구조적이고 유연한 설계를 통해, 룸 분기, 메시지 송수신, 비동기 처리, 전송 필터링 등을 모듈화하여 제공합니다.
</p>

개발 기간 : 2025년 6월 3일 ~ 2025년 6월 4일 (2일)

## 예상 인프라 아키텍처
<img src="img/architecture.png" width="800">

## 개요

ChattingEngine은 다음과 같은 기능을 중심으로 설계되었습니다:

* 참여자(Room-Participant) 구조 관리
* 소켓 기반 메시지 전송 처리
* 사용자 정의 룸 선택 및 송신 필터 정책
* ThreadPoolExecutor 기반 비동기 메시지 처리
* 메시지 직렬화 전략 및 후속 브로드캐스트 처리 분리

## 주요 구성 요소

### ChattingEngine

채팅 엔진의 진입점이며, 설정, 참여/퇴장, 리소스 초기화 등을 담당합니다.

#### 사용 예시

```java
ChattingEngine engine = new ChattingEngine();
engine.setConfig(config -> config
    .roomSelector(new CustomRoomSelector())
    .threadPool(20, 100)
    .sendFilterPolicy(new ChattingEngineConfig.BroadcastExceptSelf())
    .useInvertedIndexSessionStore(true)
);
engine.start();
```

### ChattingEngineConfig

모든 설정을 구성하는 객체로, 람다 방식의 체이닝을 통해 Builder 없이 선언적으로 사용할 수 있습니다.

#### 주요 설정 항목

* `roomSelector(RoomSelector<T>)`: 룸 선택 전략 설정
* `threadPool(int coreSize, int maxSize)`: 스레드풀 설정
* `sendFilterPolicy(SendFilterPolicy...)`: 송신 대상 필터링 정책 설정
* `messageBroadcaster(MessageBroadcaster)`: 후속 처리 브로드캐스터 설정
* `useInvertedIndexSessionStore(boolean)`: 소켓 → 룸 역방향 조회 저장소 설정

### RoomSelector<T>

소켓 및 컨텍스트 객체를 기반으로 룸 ID를 결정하는 전략 인터페이스입니다.

```java
public class UserIdBasedRoomSelector implements RoomSelector<UserContext> {
    public long selectRoom(Socket socket, UserContext context) {
        return context.getUserId() % 10;
    }
}
```

### SendFilterPolicy

수신 대상 소켓을 필터링하기 위한 인터페이스입니다. `and`, `or`, `negate` 등의 조합 메서드를 제공합니다.

```java
SendFilterPolicy onlyOthers = (receiver, sender) -> !receiver.equals(sender);
```

기본 제공 정책:

* `BroadcastExceptSelf`: 송신자 본인을 제외
* `NotConnected`: 연결된 소켓만 허용

### InMemorySessionStore

채팅방 ID와 소켓 간의 관계를 저장하는 in-memory 저장소입니다.
옵션에 따라 `Socket → RoomId` 역방향 인덱스를 활성화할 수 있습니다.

### ChatManager

* `SendFilterPolicy` 기반 필터링
* `MessageWriter<T>` 기반 직렬화
* `ThreadPoolExecutor` 기반 비동기 처리
* 메시지 전송 후 `MessageBroadcaster` 실행

```java
chatManager.send(socket, roomId, message, new JsonMessageWriter<>());
```

### MessageWriter<T>

직렬화 전략을 정의하는 함수형 인터페이스입니다. OutputStream에 메시지를 기록합니다.

```java
public class JsonMessageWriter<T> implements MessageWriter<T> {
    public void write(T message, OutputStream out) throws IOException {
        out.write(toJson(message).getBytes(StandardCharsets.UTF_8));
        out.flush();
    }
}
```

## 세션 저장소 동작 방식

| 방식 | 기능                                     |
| -- | -------------------------------------- |
| 기본 | `Map<Long, Set<Socket>>` 기반 룸 인덱스      |
| 확장 | `Map<Socket, Set<Long>>` 역방향 인덱스 동시 유지 |

* 기본 저장소는 CopyOnWriteArraySet을 사용하여 쓰기 빈도가 높을 경우 주의 필요
* 역방향 인덱스는 InvertedIndexSessionStore로 구현되며, enableReverseLookup() 호출 시 활성화

## 메시지 전송 흐름

1. `ChattingEngine.participate()` → 소켓 등록
2. `ChatManager.send()` → 필터링된 대상에게 메시지 직렬화 및 송신
3. 메시지 전송 실패는 내부 로깅 처리 (예외 전파 없음)
4. `MessageBroadcaster`를 통해 후속 처리 가능

## 예외 및 핸들링

현재는 송신 실패 및 참여자 없음 등의 상황에서 예외를 외부에 노출하지 않으며, `NoParticipantException` 등은 향후 확장 고려 대상입니다.

## 확장 및 테스트

* 단위 테스트를 위한 MockSocket, DummyWriter 등의 확장 가능
* 저장소 또는 브로드캐스터를 Redis, Kafka 등으로 대체 가능
* FilterPolicy 조합을 통해 세밀한 송수신 제어 가능

