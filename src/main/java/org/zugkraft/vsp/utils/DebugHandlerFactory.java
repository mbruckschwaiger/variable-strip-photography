package org.zugkraft.vsp.utils;

import org.opencv.core.Mat;

import java.io.IOException;
import java.nio.file.Path;

public class DebugHandlerFactory {

	public static DebugHandler get(Path debugDirectory) throws IOException {
		return debugDirectory != null ? new DefaultDebugHandler(debugDirectory) : new DebugHandler() {
			@Override
			public void write(Mat image, String imgFileName) {
			}

			@Override
			public void addToLog(int value) {
			}
		};
	}
}
