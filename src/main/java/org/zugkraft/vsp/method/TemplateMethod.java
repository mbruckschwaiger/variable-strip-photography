package org.zugkraft.vsp.method;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.bgsegm.BackgroundSubtractorCNT;
import org.opencv.bgsegm.Bgsegm;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.util.OptionalInt;

public final class TemplateMethod implements VspMethod {

	private static final int MATCH_STRIP_HALF_WIDTH = 10;
	private static final int MATCH_AREA_HALF_WIDTH = 200;

	private final BackgroundSubtractorCNT bgSubtractor;

	private Mat lastImage = null;

	public TemplateMethod() {
		Loader.load(opencv_java.class);
		bgSubtractor = Bgsegm.createBackgroundSubtractorCNT();
		bgSubtractor.setUseHistory(true);
	}

	@Override
	public OptionalInt getStripOffset(Mat image) {
		if (lastImage == null) {
			lastImage = image;
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
		var matchMask = fg.submat(0, image.rows(), center - MATCH_STRIP_HALF_WIDTH, center + MATCH_STRIP_HALF_WIDTH + 1);

		if (Core.countNonZero(matchMask) == 0)
			return 0;

		var matchStrip = image.submat(0, image.rows(), center - MATCH_STRIP_HALF_WIDTH, center + MATCH_STRIP_HALF_WIDTH + 1);
		var matchArea = lastImage.submat(0, lastImage.rows(), center - MATCH_AREA_HALF_WIDTH, center + MATCH_AREA_HALF_WIDTH + 1);

		var result = new Mat();
		Imgproc.matchTemplate(matchArea, matchStrip, result, Imgproc.TM_SQDIFF, matchMask);
		var minMaxLoc = Core.minMaxLoc(result);
		var centerX = (int)minMaxLoc.minLoc.x + MATCH_STRIP_HALF_WIDTH;
		return centerX - MATCH_AREA_HALF_WIDTH;
	}
}
