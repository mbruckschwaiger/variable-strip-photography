package org.zugkraft.vsp.utils;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class DefaultDebugHandler implements DebugHandler {

	private final BufferedWriter writer;
	private final Path debugDirectory;

	public DefaultDebugHandler(Path debugDirectory) throws IOException {
		this.debugDirectory = debugDirectory;
		this.writer = isActive() ? Files.newBufferedWriter(Path.of(String.valueOf(debugDirectory), "debug.log"), StandardCharsets.UTF_8) : null;
	}

	public boolean isActive() {
		return this.debugDirectory != null;
	}

	public void write(Mat image, String imgFileName) {
		if (!isActive())
			return;

		var path = Path.of(debugDirectory.toString(), imgFileName);
		Imgcodecs.imwrite(path.toString(), image);
	}

	public void addToLog(int value) {
		if (!isActive())
			return;

		try {
			writer.write(Integer.toString(value));
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
