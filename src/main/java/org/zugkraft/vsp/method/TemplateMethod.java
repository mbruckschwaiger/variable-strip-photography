package org.zugkraft.vsp.method;

import org.opencv.bgsegm.Bgsegm;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.BackgroundSubtractor;
import org.zugkraft.vsp.utils.DebugHandler;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public final class TemplateMethod implements VspMethod {

	private static final int MAX_OFFSET = 300;

	private final DebugHandler debugHandler;

	private BackgroundSubtractor bgSubtractor;

	private Mat lastImage = null;
	private Mat firstImageHist = null;
	private int matchStripHalfWidth;
	private int matchAreaHalfWidth;
	private double learningRate;

	TemplateMethod(DebugHandler debugHandler) {
		this.debugHandler = debugHandler;
	}

	private void initialize(Mat image) {
		bgSubtractor = Bgsegm.createBackgroundSubtractorLSBP();
		learningRate = -1.0;
		lastImage = image.clone();
		matchAreaHalfWidth = image.width() / 3;
		matchStripHalfWidth = matchAreaHalfWidth - MAX_OFFSET / 2;
	}

	@Override
	public OptionalInt getStripOffset(Mat image) {
		if (lastImage == null) {
			initialize(image);
			return OptionalInt.empty();
		}
		if (firstImageHist == null) {
			firstImageHist = calculateHistogram(image);
		}
		var offset = findOffset(image);
		lastImage = image.clone();
		return offset;
	}

	private OptionalInt findOffset(Mat image) {
		var center = image.width() / 2;
		var fgMask = new Mat();
		if (learningRate != 0.0 && !isSimilarToFirstImage(image)) learningRate = 0.0;
		bgSubtractor.apply(image, fgMask, learningRate);
		var matchMask = fgMask.submat(0, image.rows(), center - matchStripHalfWidth, center + matchStripHalfWidth + 1);

		if (Core.countNonZero(matchMask) == 0) {
			debugHandler.write(Map.of());
			debugHandler.addToLog(0);
			return OptionalInt.empty();
		}


		var matchStrip = image.submat(0, image.rows(), center - matchStripHalfWidth, center + matchStripHalfWidth + 1);
		var matchArea = lastImage.submat(0, lastImage.rows(), center - matchAreaHalfWidth, center + matchAreaHalfWidth + 1);

		var result = new Mat();
		Imgproc.matchTemplate(matchArea, matchStrip, result, Imgproc.TM_CCORR_NORMED, matchMask);
		var minMaxLoc = Core.minMaxLoc(result);
		var centerX = (int)Math.round(minMaxLoc.maxLoc.x) + matchStripHalfWidth;
		int offset = centerX - matchAreaHalfWidth;
		handleDebug(fgMask, matchMask, matchArea, matchStrip, result);
		debugHandler.addToLog(offset);
		return offset == 0 ? OptionalInt.empty() : OptionalInt.of(offset);
	}

	private boolean isSimilarToFirstImage(Mat image) {
		var imageHist = calculateHistogram(image);
		var similarity = Imgproc.compareHist(firstImageHist, imageHist, Imgproc.HISTCMP_CORREL);
		return similarity > 0.9;
	}

	private static Mat calculateHistogram(Mat image) {
		float[] histRange = {0, 256, 0, 256, 0, 256};
		int[] histSize = {256, 256, 256};
		int[] channels = {0, 1, 2};
		var imageHist = new Mat();
		Imgproc.calcHist(List.of(image), new MatOfInt(channels), new Mat(), imageHist, new MatOfInt(histSize), new MatOfFloat(histRange), false);
		return imageHist;
	}

	private void handleDebug(Mat fgMask, Mat matchMask, Mat matchArea, Mat matchStrip, Mat result) {
		if (!debugHandler.isActive())
			return;

		var normalizedResult = new Mat();
		Core.normalize(result, normalizedResult, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1);
		var strip = new Mat();
		Core.bitwise_and(matchStrip, matchStrip, strip, matchMask);

		debugHandler.write(
				Map.of(
						"matchArea", matchArea,
						"mask", fgMask,
						"matchMask", strip,
						"result", normalizedResult
				)
		);
	}
}
