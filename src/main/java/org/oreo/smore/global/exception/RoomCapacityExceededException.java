package org.oreo.smore.global.exception;

public class RoomCapacityExceededException extends RuntimeException {
    private final Long roomId;
    private final int currentCount;
    private final int maxCapacity;

    public RoomCapacityExceededException(Long roomId, int currentCount, int maxCapacity) {
        super(String.format("방 정원 초과 - 방ID: %d, 현재인원: %d, 최대인원: %d", roomId, currentCount, maxCapacity));
        this.roomId = roomId;
        this.currentCount = currentCount;
        this.maxCapacity = maxCapacity;
    }
}