package org.zugkraft.vsp;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.zugkraft.vsp.method.VspMethod;

public class VariableStripPhoto {

	private final VspMethod method;
	private final StripCollector stripCollector = new StripCollector();
	private final ImageStitcher imageStitcher = new ImageStitcher();

	static {
		Loader.load(opencv_java.class);
	}

	public VariableStripPhoto(VspMethod method) {
		this.method = method;
	}

	public void createStripPhoto(FileHandler fh) {
		var vc = new VideoCapture(fh.inputVideo().toString());
		if (!vc.isOpened()) {
			throw new RuntimeException("could not open input video file");
		}

		var frameCount = (int)vc.get(Videoio.CAP_PROP_FRAME_COUNT);
		var frameWidth = (int)vc.get(Videoio.CAP_PROP_FRAME_WIDTH);

		var stripPos = frameWidth / 2;
		for (var i = 0; i < frameCount; i++) {
			var image = new Mat();
			vc.read(image);
			var offset = method.getStripOffset(image);
			offset.ifPresent(o -> stripCollector.add(image, stripPos, o));
			printProgress(i+1, frameCount, offset.orElse(0));
		}
		System.out.println();

		var result = imageStitcher.stitch(stripCollector);
		Imgcodecs.imwrite(fh.resultImage().toString(), result);
	}

	private static void printProgress(int current, int total, double calculatedValue) {
		int width = 50;

		var progress = (double) current / total;
		var filled = (int) (progress * width);

		var bar = "[" + "=".repeat(filled) + " ".repeat(width - filled) + "]";

		System.out.printf(
				"\r%s %6.2f%% (%d/%d) : %6.2f",
				bar,
				progress * 100,
				current,
				total,
				calculatedValue
		);

		System.out.flush();
	}
}
