package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.zugkraft.vsp.utils.DebugHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public final class TemplateMethod implements VspMethod {

	record BackgroundModel(Mat mean, Mat stdDev) {}

	private static final int MAX_OFFSET = 300;

	private final DebugHandler debugHandler;

	private final List<Mat> backgroundFrames = new ArrayList<>();
	private BackgroundModel backgroundModel = null;
	private Mat firstImageHist = null;
	private Mat lastImage = null;
	private int matchStripHalfWidth;
	private int matchAreaHalfWidth;

	TemplateMethod(DebugHandler debugHandler) {
		this.debugHandler = debugHandler;
	}

	private void initialize(Mat image) {
		lastImage = image.clone();
		matchAreaHalfWidth = image.width() / 3;
		matchStripHalfWidth = matchAreaHalfWidth - MAX_OFFSET / 2;
	}

	@Override
	public OptionalDouble getStripOffset(Mat image) {
		if (lastImage == null) {
			initialize(image);
			return OptionalDouble.empty();
		}
		if (firstImageHist == null) {
			firstImageHist = calculateHistogram(image);
		}
		if (backgroundModel == null) {
			if (isSimilarToFirstImage(image)) {
				backgroundFrames.add(image.clone());
				debugHandler.write(Map.of());
				debugHandler.addToLog(0);
				return OptionalDouble.empty();
			}
			else {
				backgroundModel = calculateBackgroundModel(backgroundFrames);
			}
		}

		var offset = findOffset(image);
		lastImage = image.clone();
		return offset;
	}

	private OptionalDouble findOffset(Mat image) {
		var center = image.width() / 2;
		var fgMask = createAdaptiveForegroundMask(image, backgroundModel, 10.5, 50.0);
		var matchMask = fgMask.submat(0, image.rows(), center - matchStripHalfWidth, center + matchStripHalfWidth + 1);

		if (Core.countNonZero(matchMask) == 0) {
			debugHandler.write(Map.of());
			debugHandler.addToLog(0);
			return OptionalDouble.empty();
		}


		var matchStrip = image.submat(0, image.rows(), center - matchStripHalfWidth, center + matchStripHalfWidth + 1);
		var matchArea = lastImage.submat(0, lastImage.rows(), center - matchAreaHalfWidth, center + matchAreaHalfWidth + 1);

		var result = new Mat();
		Imgproc.matchTemplate(matchArea, matchStrip, result, Imgproc.TM_CCORR_NORMED, matchMask);
		var minMaxLoc = Core.minMaxLoc(result);
		var peak = minMaxLoc.maxLoc;
		var subPixelX = subPixelPeakX(result, peak);
		var centerX = subPixelX + matchStripHalfWidth;
		var offset = centerX - matchAreaHalfWidth;
		handleDebug(fgMask, matchMask, matchArea, matchStrip, result);
		debugHandler.addToLog(offset);
		return isZero(offset) ? OptionalDouble.empty() : OptionalDouble.of(offset);
	}

	private static double subPixelPeakX(Mat response, Point peak) {
		int x = (int)peak.x;
		int y = (int)peak.y;

		if (x <= 0 || x >= response.cols() - 1) {
			return peak.x;
		}

		double left   = response.get(y, x - 1)[0];
		double center = response.get(y, x    )[0];
		double right  = response.get(y, x + 1)[0];

		double denominator = left - 2.0 * center + right;

		if (Math.abs(denominator) < 1e-12) {
			return peak.x;
		}

		double dx = 0.5 * (left - right) / denominator;

		if (Math.abs(dx) > 1.0) {
			return peak.x;
		}

		return x + dx;
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

	private static BackgroundModel calculateBackgroundModel(List<Mat> frames) {
		if (frames.isEmpty()) {
			throw new IllegalArgumentException("No background frames supplied");
		}

		Size size = frames.getFirst().size();

		Mat sum = Mat.zeros(size, CvType.CV_32FC3);
		Mat sumSquares = Mat.zeros(size, CvType.CV_32FC3);

		Mat floatFrame = new Mat();
		Mat squared = new Mat();

		for (Mat frame : frames) {
			frame.convertTo(floatFrame, CvType.CV_32FC3);

			Core.add(sum, floatFrame, sum);

			Core.multiply(floatFrame, floatFrame, squared);
			Core.add(sumSquares, squared, sumSquares);
		}

		double count = frames.size();

		Mat mean = new Mat();
		Core.divide(sum, new Scalar(count, count, count), mean);

		Mat meanSquares = new Mat();
		Core.divide(sumSquares, new Scalar(count, count, count), meanSquares);

		Mat squaredMean = new Mat();
		Core.multiply(mean, mean, squaredMean);

		Mat variance = new Mat();
		Core.subtract(meanSquares, squaredMean, variance);

		Core.max(variance, new Scalar(0, 0, 0), variance);

		Mat stdDev = new Mat();
		Core.sqrt(variance, stdDev);

		sum.release();
		sumSquares.release();
		floatFrame.release();
		squared.release();
		meanSquares.release();
		squaredMean.release();
		variance.release();

		return new BackgroundModel(mean, stdDev);
	}

	private static Mat createAdaptiveForegroundMask(
			Mat frame,
			BackgroundModel model,
			double sigmaThreshold,
			double minimumDifference
	) {
		var floatFrame = new Mat();
		frame.convertTo(floatFrame, CvType.CV_32FC3);

		var difference = new Mat();
		Core.absdiff(floatFrame, model.mean(), difference);

		var threshold = new Mat();
		Core.multiply(
				model.stdDev(),
				new Scalar(sigmaThreshold, sigmaThreshold, sigmaThreshold),
				threshold
		);

		Core.add(
				threshold,
				new Scalar(minimumDifference, minimumDifference, minimumDifference),
				threshold
		);

		var channelMask = new Mat();
		Core.compare(difference, threshold, channelMask, Core.CMP_GT);

		var channels = new ArrayList<Mat>(3);
		Core.split(channelMask, channels);

		var mask = new Mat();

		Core.bitwise_or(channels.get(0), channels.get(1), mask);
		Core.bitwise_or(mask, channels.get(2), mask);

		var kernel = Imgproc.getStructuringElement(
				Imgproc.MORPH_ELLIPSE,
				new Size(5, 5)
		);

		Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_OPEN, kernel);
		Imgproc.morphologyEx(mask, mask, Imgproc.MORPH_CLOSE, kernel);

		floatFrame.release();
		difference.release();
		threshold.release();
		channelMask.release();
		kernel.release();

		channels.forEach(Mat::release);

		return mask;
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
