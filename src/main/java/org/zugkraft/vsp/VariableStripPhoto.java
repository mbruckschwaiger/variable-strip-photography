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

		var columns = new ArrayList<Strip>();
		var pos = 0;
		for (var i = 0; i < frameCount; i++) {
			var image = new Mat();
			vc.read(image);
			var strip = method.getStrip(image);
			columns.add(new Strip(strip, pos));
			pos += strip.width();
			if (i % 100 == 0)
				System.out.println("processing frame " + i);
		}

		columns.sort(Comparator.comparingInt(Strip::pos));
		var firstStrip = columns.getFirst().strip();
		var result = new Mat(firstStrip.height(), calculateWidth(columns), firstStrip.type());
		for (var s : columns) {
			var strip = s.strip();
			var destinationRoi = new Rect(s.pos(), 0, strip.width(), strip.height());
			var resultMat = result.submat(destinationRoi);
			strip.copyTo(resultMat);
		}

		Imgcodecs.imwrite(outputImage, result);
	}

	private int calculateWidth(List<Strip> columns) {
		var minX = columns.getFirst().pos();
		var maxX = columns.stream()
				.mapToInt(c -> c.pos() + c.strip().width())
				.max().orElse(-1);

		return maxX - minX;
	}
}
