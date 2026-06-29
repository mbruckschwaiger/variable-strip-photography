package org.zugkraft.vsp;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.ArrayList;
import java.util.Comparator;

public class VariableStripPhoto {

	public VariableStripPhoto() {
		Loader.load(opencv_java.class);
	}

	public void createStripPhoto(String inputVideo, String outputImage) {
		var vc = new VideoCapture(inputVideo);
		if (!vc.isOpened()) {
			throw new RuntimeException("could not open input video file");
		}

		var frameCount = (int)vc.get(Videoio.CAP_PROP_FRAME_COUNT);

		var columns = new ArrayList<Strip>();
		for (var pos = 0; pos < frameCount; pos++) {
			var image = new Mat();
			vc.read(image);
			var strip = getStrip(image);
			columns.add(new Strip(strip, pos));
			if (pos % 100 == 0)
				System.out.println("processing frame " + pos);
		}

		columns.sort(Comparator.comparingInt(Strip::pos));
		var firstStrip = columns.getFirst().strip();
		var result = new Mat(firstStrip.height(), columns.size(), firstStrip.type());
		for (var s : columns) {
			var strip = s.strip();
			var x = s.pos();
			var destinationRoi = new Rect(x, 0, 1, strip.height());
			var resultMat = result.submat(destinationRoi);
			strip.copyTo(resultMat);
		}

		Imgcodecs.imwrite(outputImage, result);
	}

	private Mat getStrip(Mat image) {
		var center = image.width() / 2;
		return new Mat(image, new Range(0, image.height()), new Range(center, center + 1));
	}
}
