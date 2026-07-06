package org.zugkraft.vsp;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.zugkraft.vsp.method.VspMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class VariableStripPhoto {

	private final VspMethod method;

	public VariableStripPhoto(VspMethod method) {
		Loader.load(opencv_java.class);
		this.method = method;
	}

	public void createStripPhoto(String inputVideo, String outputImage) {
		var vc = new VideoCapture(inputVideo);
		if (!vc.isOpened()) {
			throw new RuntimeException("could not open input video file");
		}

		var frameCount = (int)vc.get(Videoio.CAP_PROP_FRAME_COUNT);
		var frameWidth = (int)vc.get(Videoio.CAP_PROP_FRAME_WIDTH);

		var columns = new ArrayList<Strip>();
		var pos = 0;
		var center = frameWidth / 2;
		for (var i = 0; i < frameCount; i++) {
			var image = new Mat();
			vc.read(image);
			var offset = method.getStripOffset(image);
			var finalPos = pos;
			offset.ifPresent(o -> columns.add(new Strip(createStrip(image, center, o), finalPos)));
			pos += offset.orElse(0);
			if (i % 100 == 0)
				System.out.println("processing frame " + i);
		}

		columns.sort(Comparator.comparingInt(Strip::pos));
		var firstColumn = columns.getFirst();
		var firstStrip = firstColumn.strip();
		var minX = firstColumn.pos();
		var result = new Mat(firstStrip.height(), calculateWidth(columns, minX), firstStrip.type());
		for (var s : columns) {
			var strip = s.strip();
			var destinationRoi = new Rect(s.pos() - minX, 0, strip.width(), strip.height());
			var resultMat = result.submat(destinationRoi);
			strip.copyTo(resultMat);
		}

		Imgcodecs.imwrite(outputImage, result);
	}

	private int calculateWidth(List<Strip> columns, int minX) {
		var maxX = columns.stream()
				.mapToInt(c -> c.pos() + c.strip().width())
				.max().orElse(-1);

		return maxX - minX;
	}

	private Mat createStrip(Mat image, int center, int offset) {
		int left = Math.min(center, center + offset);
		return new Mat(image, new Rect(left, 0, Math.abs(offset), image.height()));
	}
}
