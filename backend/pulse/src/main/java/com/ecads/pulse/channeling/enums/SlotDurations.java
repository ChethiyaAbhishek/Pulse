package com.ecads.pulse.channeling.enums;

import lombok.Getter;

@Getter
public enum SlotDurations {
    M15(15),
    M30(30),
    M45(45),
    M60(60);

    private final int minutes;

    SlotDurations(int minutes) {
        this.minutes = minutes;
    }
}
