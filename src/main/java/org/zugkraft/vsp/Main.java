package org.zugkraft.vsp;

import org.zugkraft.vsp.method.VspMethodFactory;
import org.zugkraft.vsp.utils.DebugHandlerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class Main {
	static void main(String[] args) throws IOException {
		if (args.length != 3)
			exitWithUsage();

		var configPath = Path.of(args[0]);
		var inputVideo = args[1];
		var outputDirectory = args[2];

		var properties = new Properties();
		try (var reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			properties.load(reader);
		}

		var fh = FileHandler.of(inputVideo, outputDirectory, properties);

		try (var dh = DebugHandlerFactory.get(fh.debugDirectory())) {
			var method = VspMethodFactory.createMethod(properties, dh);
			var vsp = new VariableStripPhoto(method);


			System.out.println("input video     : " + fh.inputVideo());
			System.out.println("output image    : " + fh.resultImage());
			System.out.println("debug directory : " + fh.debugDirectory());

			System.out.println(properties);
			System.out.println();

			vsp.createStripPhoto(fh);
		}
	}

	private static void exitWithUsage() {
		System.out.println("""
				Usage:
					VSP path/to/vsp.properties path/to/input/video/file(s) path/to/output/directory
				""");
		System.exit(1);
	}
}
