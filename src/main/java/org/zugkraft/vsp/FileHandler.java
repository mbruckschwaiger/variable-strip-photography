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
		var method = getMethodPostfix(p);
		var baseFileNameComponent = getBaseFileNameComponent(iv);
		var outputBaseName = baseFileNameComponent + "_double_" + method;
		var ri = Path.of(outputDirectory, outputBaseName + ".png");
		var dd = Path.of(outputDirectory, outputBaseName);
		if (debug && !Files.exists(dd)) {
			Files.createDirectories(dd);
		}
		return new FileHandler(iv, ri, debug ? dd : null);
	}

	private static String getMethodPostfix(Properties p) {
		var method = p.getProperty("vsp.method");
		var extension = "simple".equalsIgnoreCase(method) ? "_" + p.getProperty("vsp.simple.width") :
				("flow".equalsIgnoreCase(method) ? "_" + p.getProperty("vsp.flow.algorithm") : "");
		return method + extension;
	}

	private static String getBaseFileNameComponent(Path videoFile) {
		var videoFileName = videoFile.getFileName().toString();
		return videoFileName
				.replaceAll("%\\d+\\D", "")
				.replaceAll("\\.(?=[^.]*$).*", "");
	}
}
