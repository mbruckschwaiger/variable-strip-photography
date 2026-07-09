package org.zugkraft.vsp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record FileHandler(Path inputVideo, Path resultImage, Path debugDirectory) {

	public static FileHandler of(String inputVideo, String outputDirectory, Properties p) throws IOException {
		var debug = Boolean.parseBoolean(p.getProperty("vsp.debug"));
		var iv = Path.of(inputVideo);
		var outDirPath = Path.of(outputDirectory);
		if (!Files.exists(outDirPath)) {
			Files.createDirectories(outDirPath);
		}
		var timestamp = String.valueOf(System.currentTimeMillis());
		var ri = setupOutputImage(iv, outputDirectory, timestamp);
		var dd = Path.of(outputDirectory, "debug_" + timestamp);
		if (debug && !Files.exists(dd)) {
			Files.createDirectories(dd);
		}
		return new FileHandler(iv, ri, debug ? dd : null);
	}

	private static Path setupOutputImage(Path inputVideo, String outputDirectory, String timestamp) {
		var videoFileName = inputVideo.getFileName().toString();
		var imageFileName = videoFileName.replace('%', '_').replace('.', '_') + "_" + timestamp + ".png";
		return Path.of(outputDirectory, imageFileName);
	}

}
