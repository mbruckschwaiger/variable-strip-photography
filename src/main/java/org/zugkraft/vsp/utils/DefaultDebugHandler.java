package org.zugkraft.vsp.utils;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

class DefaultDebugHandler implements DebugHandler {

	private final BufferedWriter writer;
	private final Path debugDirectory;
	private int writeIndex = 0;

	public DefaultDebugHandler(Path debugDirectory) throws IOException {
		this.debugDirectory = debugDirectory;
		this.writer = isActive() ? Files.newBufferedWriter(Path.of(String.valueOf(debugDirectory), "debug.log"), StandardCharsets.UTF_8) : null;
	}

	public boolean isActive() {
		return this.debugDirectory != null;
	}

	public void write(Map<String, Mat> images) {
		if (!isActive())
			return;

		var fileBaseName = String.format("%09d_", writeIndex++);
		for (var entry : images.entrySet()) {
			var name = fileBaseName + entry.getKey() + ".png";
			var img = entry.getValue();
			var path = Path.of(debugDirectory.toString(), name);
			Imgcodecs.imwrite(path.toString(), img);
		}
	}

	public void addToLog(double value) {
		if (!isActive())
			return;

		try {
			writer.write(Double.toString(value));
			writer.newLine();
		} catch (IOException e) {
			System.err.println("Error writing to log file");
		}
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}
}
