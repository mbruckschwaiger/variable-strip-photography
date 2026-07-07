package org.zugkraft.vsp.method;

import org.opencv.bgsegm.Bgsegm;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;

import java.util.OptionalInt;

public final class TemplateMethod implements VspMethod {

	private static final int MAX_OFFSET = 300;

	private BackgroundSubtractor bgSubtractor;

	private Mat lastImage = null;
	private int matchStripHalfWidth;
	private int matchAreaHalfWidth;
	private Mat kernel = null;

	private void initialize(Mat image) {
		bgSubtractor = Bgsegm.createBackgroundSubtractorGSOC();
		lastImage = image;
		matchAreaHalfWidth = image.width() / 3;
		matchStripHalfWidth = matchAreaHalfWidth - MAX_OFFSET / 2;
		kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(95, 95));
	}

	@Override
	public OptionalInt getStripOffset(Mat image) {
		if (lastImage == null) {
			initialize(image);
			return OptionalInt.empty();
		}
		var offset = findOffset(lastImage, image);
		if (offset.isPresent())
			lastImage = image;
		return offset;
	}

	private OptionalInt findOffset(Mat lastImage, Mat image) {
		var center = image.width() / 2;
		var fgMask = new Mat();
		bgSubtractor.apply(image, fgMask);
		var fg = new Mat();
		Imgproc.morphologyEx(fgMask, fg, Imgproc.MORPH_CLOSE, kernel);
		var matchMask = fg.submat(0, image.rows(), center - matchStripHalfWidth, center + matchStripHalfWidth + 1);

		if (Core.countNonZero(matchMask) == 0)
			return OptionalInt.empty();

		var matchStrip = image.submat(0, image.rows(), center - matchStripHalfWidth, center + matchStripHalfWidth + 1);
		var matchArea = lastImage.submat(0, lastImage.rows(), center - matchAreaHalfWidth, center + matchAreaHalfWidth + 1);

		var result = new Mat();
		Imgproc.matchTemplate(matchArea, matchStrip, result, Imgproc.TM_CCORR_NORMED, matchMask);
		var minMaxLoc = Core.minMaxLoc(result);
		var centerX = (int)Math.round(minMaxLoc.maxLoc.x) + matchStripHalfWidth;
		int offset = centerX - matchAreaHalfWidth;
		return offset == 0 ? OptionalInt.empty() : OptionalInt.of(offset);
	}
}
