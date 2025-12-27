package com.chessnalysis.domain.game;

import lombok.Getter;

/**
 * Time control presets for chess games.
 * Each preset defines base time in seconds and increment per move in seconds.
 */
@Getter
public enum TimeControl {
	BULLET_1_0(1, 0, "Bullet 1+0"),
	BULLET_2_1(2, 1, "Bullet 2+1"),
	BLITZ_3_0(3, 0, "Blitz 3+0"),
	BLITZ_3_2(3, 2, "Blitz 3+2"),
	BLITZ_5_0(5, 0, "Blitz 5+0"),
	BLITZ_5_3(5, 3, "Blitz 5+3"),
	RAPID_10_0(10, 0, "Rapid 10+0"),
	RAPID_15_10(15, 10, "Rapid 15+10"),
	CLASSICAL_30_0(30, 0, "Classical 30+0");

	private final int baseSeconds;
	private final int incrementSeconds;
	private final String displayName;

	TimeControl(int baseSeconds, int incrementSeconds, String displayName) {
		this.baseSeconds = baseSeconds;
		this.incrementSeconds = incrementSeconds;
		this.displayName = displayName;
	}

    public int getTotalMillisPerSide() {
		return baseSeconds * 1000;
	}
}

