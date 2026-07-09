package org.zugkraft.vsp.utils;

import org.opencv.core.Mat;

import java.io.IOException;

public interface DebugHandler extends AutoCloseable {

	default boolean isActive() {
		return false;
	}

	@Override
	default void close() throws IOException {
	}

	void write(Mat image, String imgFileName);
	void addToLog(int value);
}
