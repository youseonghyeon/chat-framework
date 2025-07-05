package io.github.youseonghyeon;

import io.github.youseonghyeon.core.ChatEngine;

public class EngineStarter {

    public static void main(String[] args) {
        ChatEngine chatEngine = new ChatEngine();
        chatEngine.setConfig(config -> config.port(9999));

        chatEngine.start();
    }


}
