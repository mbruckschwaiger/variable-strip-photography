package org.zugkraft.vsp.utils;

import org.opencv.core.Mat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface DebugHandler extends AutoCloseable {

	default boolean isActive() {
		return false;
	}

	@Override
	default void close() throws IOException {
	}

	void write(Map<String, Mat> images);
	void writeHistograms(Map<String, List<Double>> histograms);
	void addToLog(double value);
	void addToLog(double value, double weight);
}
