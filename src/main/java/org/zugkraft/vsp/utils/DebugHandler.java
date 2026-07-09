package org.zugkraft.vsp.utils;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.nio.file.Path;

public class DebugHandler {

	private final Path debugDirectory;

	public DebugHandler(Path debugDirectory) {
		this.debugDirectory = debugDirectory;
	}

	public boolean isActive() {
		return this.debugDirectory != null;
	}

	public void write(Mat image, String imgFileName) {
		var path = Path.of(debugDirectory.toString(), imgFileName);
		Imgcodecs.imwrite(path.toString(), image);
	}
}
