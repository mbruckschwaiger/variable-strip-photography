package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.zugkraft.vsp.StripInfo;

public final class TemplateMethod implements VspMethod {

	private static final int MATCH_STRIP_HALF_WIDTH = 10;
	private static final int MATCH_AREA_HALF_WIDTH = 200;


	@Override
	public StripInfo getStrip(Mat lastImage, Mat image) {
		if (lastImage == null) {
			return null;
		}
		var center = image.width() / 2;
		var offset = findOffset(lastImage, image);
		if (offset == 0) return null;
		var w = Math.abs(offset);
		var stripRoi = new Rect(center - w / 2, 0, w, image.height());
		var strip = new Mat(image, stripRoi);
		return new StripInfo(strip, offset >= 0 ? 1 : -1);
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
