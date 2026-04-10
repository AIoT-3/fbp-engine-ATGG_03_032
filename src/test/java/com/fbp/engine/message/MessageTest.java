package com.fbp.engine.message;

import com.fbp.engine.message.exception.NotFoundPayloadKeyException;
import org.junit.jupiter.api.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageTest {
    private Message tm;
    private Map<String, Object> payload;

    @BeforeEach
    void setUp(){
        payload = new HashMap<>();
        payload.put("temperature", 25.5);
        payload.put("co2", 1200);
        payload.put("pressure", 1005);
        tm = new Message(payload);
    }

    @Order(1)
    @Test
    @DisplayName("생성 시 ID 자동 할당")
    void shouldAssignIdAutomaticallyOnCreation(){
        String id = String.valueOf(tm.getId());
        assertTrue(id != null && !id.isEmpty());
    }

    @Order(2)
    @Test
    @DisplayName("생성 시 timestamp 자동 기록")
    void shouldRecordTimestampOnCreation(){
        LocalDateTime timestamp = tm.getTimestamp();
        assertNotNull(timestamp);
        assertTrue(timestamp.isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Order(3)
    @Test
    @DisplayName("페이로드 조회")
    void shouldRetrievePayloadValueSuccessfully(){
        Map<String, Object> payload = tm.getPayload();
        Set<String> keys = payload.keySet();

        for(String key : keys){
            Assertions.assertNotNull(payload.get(key));
        }
    }


    @Order(4)
    @Test
    @DisplayName("제네릭 get 타입 캐스팅")
    void shouldCastTypeCorrectlyWhenGettingValue(){
        Assertions.assertDoesNotThrow(() -> (Double) tm.get("temperature"));
    }

    @Order(5)
    @Test
    @DisplayName("존재하지 않는 키 조회")
    void shouldReturnNullWhenKeyDoesNotExist(){
        Assertions.assertThrows(NotFoundPayloadKeyException.class,
                () -> tm.get("없는키"));
    }

    @Order(6)
    @Test
    @DisplayName("페이로드 불변 — 외부 수정 차단")
    void shouldThrowExceptionWhenModifyingPayloadDirectly(){
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> tm.getPayload().put("key", null));
    }

    @Order(7)
    @Test
    @DisplayName("페이로드 불변 — 원본 Map 수정 무영향")
    void shouldNotBeAffectedByOriginalMapModification(){
        payload.put("newKey", 101);

        Assertions.assertFalse(tm.hasKey("newKey"));
    }

    @Order(8)
    @Test
    @DisplayName("withEntry — 새 객체 반환")
    void withEntryShouldReturnNewMessageInstance(){
        Message newMessage = tm.withEntry("tmp", 100);

        Assertions.assertNotEquals(newMessage, tm);
    }

    @Order(9)
    @Test
    @DisplayName("withEntry — 원본 불변")
    void withEntryShouldNotModifyOriginalMessage(){
        tm.withEntry("newKey", 100);

        Assertions.assertFalse(tm.hasKey("newKey"));
    }

    @Order(10)
    @Test
    @DisplayName("withEntry — 새 메시지에 값 존재")
    void withEntryShouldIncludeNewValueInNewMessage(){
        Message newMessage = tm.withEntry("newKey", 200);

        Assertions.assertTrue(newMessage.hasKey("newKey"));
    }

    @Order(11)
    @Test
    @DisplayName("hasKey — 존재하는 키")
    void hasKeyShouldReturnTrueForExistingKey(){
        Assertions.assertTrue(tm.hasKey("temperature"));
    }
    @Order(12)
    @Test
    @DisplayName("hasKey — 없는 키")
    void hasKeyShouldReturnFalseForNonExistentKey(){
        Assertions.assertFalse(tm.hasKey("없는 키"));
    }
    @Order(13)
    @Test
    @DisplayName("withoutKey — 키 제거 확인")
    void withoutKeyShouldRemoveKeyFromNewMessage(){
        Message message = tm.withoutKey("temperature");

        Assertions.assertFalse(message.hasKey("temperature"));
    }
    @Order(14)
    @Test
    @DisplayName("withoutKey — 원본 불변")
    void withoutKeyShouldNotModifyOriginalMessage(){
        tm.withoutKey("temperature");

        Assertions.assertTrue(tm.hasKey("temperature"));
    }
    @Order(15)
    @Test
    @DisplayName("toString 포맷")
    void toStringShouldIncludePayloadInformation(){
        Assertions.assertNotNull(tm.toString());
        Assertions.assertTrue(tm.toString().contains(tm.getPayload().toString()));
    }
}
