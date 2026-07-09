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
		var baseFileNameComponent = getBaseFileNameComponent(iv);
		var ri = Path.of(outputDirectory, baseFileNameComponent + ".png");
		var dd = Path.of(outputDirectory, baseFileNameComponent);
		if (debug && !Files.exists(dd)) {
			Files.createDirectories(dd);
		}
		return new FileHandler(iv, ri, debug ? dd : null);
	}

	private static String getBaseFileNameComponent(Path videoFile) {
		var videoFileName = videoFile.getFileName().toString();
		return videoFileName
				.replaceAll("%\\d+\\D", "")
				.replaceAll("\\.(?=[^.]*$).*", "");
	}
}
