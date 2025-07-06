package io.github.youseonghyeon.core.dto;

public class SendResult {
    // TODO iterator 사용을 위해 Exception 던지지 않고 결과를 조합해서 처리 하는 것으로 만들어야 함

    public static SendResult emptyResult() {
        return new SendResult();
    }
}
