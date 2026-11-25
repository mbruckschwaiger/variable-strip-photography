package org.zugkraft.vsp.utils;

import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class DebugHandlerFactory {

	public static DebugHandler get(Path debugDirectory) throws IOException {
		return debugDirectory != null ? new DefaultDebugHandler(debugDirectory) : new DebugHandler() {
			@Override
			public void write(Map<String, Mat> images) {
			}

			@Override
			public void writeHistograms(Map<String, List<Double>> histograms) {
			}

			@Override
			public void addToLog(double value) {
			}

			@Override
			public void addToLog(double value, double weight) {
			}
		};
	}
}
