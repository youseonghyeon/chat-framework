package io.github.youseonghyeon;

import io.github.youseonghyeon.config.PublicSquareRoomSelector;
import io.github.youseonghyeon.config.datasource.MemoryChattingDataSource;

public class ConfigExample {


    public static void main(String[] args) {
        int port = 9999;
        ChattingEngine engine = new ChattingEngine();
        engine.setConfig(config ->
                config.datasource(new MemoryChattingDataSource())
                        .roomSelector(new PublicSquareRoomSelector()));
        engine.start();
    }

}
