package org.zugkraft.vsp.method;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.bgsegm.Bgsegm;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;

import java.util.OptionalInt;

public final class TemplateMethod implements VspMethod {

	private static final int MAX_OFFSET = 300;

	private final BackgroundSubtractor bgSubtractor;

	private Mat lastImage = null;
	private int matchStripHalfWidth;
	private int matchAreaHalfWidth;

	public TemplateMethod() {
		Loader.load(opencv_java.class);
		bgSubtractor = Bgsegm.createBackgroundSubtractorGSOC();
	}

	private void initialize(Mat image) {
		lastImage = image;
		matchAreaHalfWidth = image.width() / 3;
		matchStripHalfWidth = matchAreaHalfWidth - MAX_OFFSET / 2;
	}

	@Override
	public OptionalInt getStripOffset(Mat image) {
		if (lastImage == null) {
			initialize(image);
			return OptionalInt.empty();
		}
		var offset = findOffset(lastImage, image);
		if (offset == 0) return OptionalInt.empty();
		lastImage = image;
		return OptionalInt.of(offset);
	}

	private int findOffset(Mat lastImage, Mat image) {
		var center = image.width() / 2;
		var fg = new Mat();
		bgSubtractor.apply(image, fg);
		var matchMask = fg.submat(0, image.rows(), center - matchStripHalfWidth, center + matchStripHalfWidth + 1);

		if (Core.countNonZero(matchMask) == 0)
			return 0;

		var matchStrip = image.submat(0, image.rows(), center - matchStripHalfWidth, center + matchStripHalfWidth + 1);
		var matchArea = lastImage.submat(0, lastImage.rows(), center - matchAreaHalfWidth, center + matchAreaHalfWidth + 1);

		var result = new Mat();
		Imgproc.matchTemplate(matchArea, matchStrip, result, Imgproc.TM_CCORR_NORMED, matchMask);
		var minMaxLoc = Core.minMaxLoc(result);
		var centerX = (int)Math.round(minMaxLoc.maxLoc.x) + matchStripHalfWidth;
		return centerX - matchAreaHalfWidth;
	}
}
