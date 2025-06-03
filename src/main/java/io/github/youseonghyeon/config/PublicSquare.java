package io.github.youseonghyeon.config;

public class PublicSquare implements RoomSelector {

    @Override
    public Long selectRoom() {
        return 0L;
    }
}
