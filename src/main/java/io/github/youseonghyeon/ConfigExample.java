package io.github.youseonghyeon;

import io.github.youseonghyeon.config.PublicSquare;
import io.github.youseonghyeon.config.datasource.MemoryChattingDataSource;

public class ConfigExample {


    public static void main(String[] args) {
        int port = 9999;
        ChattingEngine engine = new ChattingEngine(port);
        engine.setConfig(config ->
                config.datasource(new MemoryChattingDataSource())
                        .roomSelector(new PublicSquare()));
        engine.start();


//        engine.participate(socket, new PublicSquare());
    }
}
