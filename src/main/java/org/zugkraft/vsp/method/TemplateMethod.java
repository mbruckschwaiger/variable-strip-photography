package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public final class TemplateMethod implements VspMethod {

	private static final int MATCH_STRIP_HALF_WIDTH = 10;
	private static final int MATCH_AREA_HALF_WIDTH = 200;

	private Mat lastImage = null;

	@Override
	public int getStripOffset(Mat image) {
		if (lastImage == null) {
			lastImage = image;
			return 0;
		}
		var offset = findOffset(lastImage, image);
		if (offset == 0) return 0;
		lastImage = image;
		return offset;
	}

	private int findOffset(Mat lastImage, Mat image) {
		var center = image.width() / 2;
		var matchStrip = image.submat(0, image.rows(), center - MATCH_STRIP_HALF_WIDTH, center + MATCH_STRIP_HALF_WIDTH + 1);
		var matchArea = lastImage.submat(0, lastImage.rows(), center - MATCH_AREA_HALF_WIDTH, center + MATCH_AREA_HALF_WIDTH + 1);

		var result = new Mat();
		Imgproc.matchTemplate(matchArea, matchStrip, result, Imgproc.TM_SQDIFF_NORMED);
		var minMaxLoc = Core.minMaxLoc(result);
		var centerX = (int)minMaxLoc.minLoc.x + MATCH_STRIP_HALF_WIDTH;
		return centerX - MATCH_AREA_HALF_WIDTH;
	}
}
