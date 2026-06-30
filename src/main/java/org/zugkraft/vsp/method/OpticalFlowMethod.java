package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.zugkraft.vsp.StripInfo;

import java.util.ArrayList;
import java.util.Arrays;

public final class OpticalFlowMethod implements VspMethod {

	@Override
	public StripInfo getStrip(Mat lastImage, Mat image) {
		if (lastImage == null) {
			return null;
		}

		var lastGray = new Mat();
		Imgproc.cvtColor(lastImage, lastGray, Imgproc.COLOR_BGR2GRAY);
		Mat gray = new Mat();
		Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

		Mat flow = new Mat(gray.size(), CvType.CV_32FC2);
		Video.calcOpticalFlowFarneback(lastGray, gray, flow, 0.5, 3, 15, 3, 5, 1.2, 0);

		var flowParts = new ArrayList<Mat>(2);
		Core.split(flow, flowParts);
		var flowX = flowParts.get(0);
		var flowY = flowParts.get(1);

		float horizontalRatio = 3.0f;
		float minMagnitude = 2.0f;

		var horizontalValues = new ArrayList<Float>();
		for (var y = 0; y < flow.rows(); y++) {
			for (var x = 0; x < flow.cols(); x++) {
				float dx = (float)flowX.get(y, x)[0];
				float dy = (float)flowY.get(y, x)[0];

				float absDx = Math.abs(dx);
				float absDy = Math.abs(dy);

				if (absDx > minMagnitude && absDx > horizontalRatio * absDy) {
					horizontalValues.add(dx);
				}
			}
		}

		double binSize = 1.0;
		double minValue = -200.0;
		double maxValue = 200.0;

		var binCount = (int)Math.ceil((maxValue - minValue) / binSize);
		var histogram = new int[binCount];

		for (var v : horizontalValues) {
			var bin = (int) ((v - minValue) / binSize);
			if (bin >= 0 && bin < binCount) {
				histogram[bin]++;
			}
		}

		System.out.println(Arrays.toString(histogram));

		int bestBin = 0;

		for (var i = 1; i < histogram.length; i++) {
			if (histogram[i] > histogram[bestBin]) {
				bestBin = i;
			}
		}
		if (histogram[bestBin] == 0) {
			return null;
		}

//		int minBin = 0;
//		int maxBin = histogram.length - 1;
//
//		while (minBin < histogram.length && histogram[minBin] == 0) minBin++;
//		while (maxBin >= 0 && histogram[maxBin] == 0) maxBin--;
//
//		if (minBin == histogram.length || maxBin == -1) {
//			return null;
//		}
//
//		double peakMagnitudeMin = minValue + (minBin + 0.5) * binSize;
//		double peakMagnitudeMax = minValue + (maxBin + 0.5) * binSize;
//
//		var peakMagnitude = Math.abs(peakMagnitudeMax) > Math.abs(peakMagnitudeMin) ? peakMagnitudeMax : peakMagnitudeMin;


		double peakMagnitude = minValue + (bestBin + 0.5) * binSize;
		var peakMagnitudeInt = (int)Math.round(peakMagnitude * (-1));
		if (peakMagnitudeInt == 0) {
			return null;
		}

//		var magnitude = new Mat();
//		var angle = new Mat();
//		Core.cartToPolar(flowX, flowY, magnitude, angle, true);
//		visualize(angle, magnitude);

		var center = image.width() / 2;
		var w = Math.abs(peakMagnitudeInt);
		var stripRoi = new Rect(center - w / 2, 0, w, image.height());
		var strip = new Mat(image, stripRoi);
		System.out.println(peakMagnitudeInt);
		return new StripInfo(strip, peakMagnitudeInt >= 0 ? 1 : -1);
	}

	private void visualize(Mat angle, Mat magnitude) {
		var magnNorm = new Mat();
		Core.normalize(magnitude, magnNorm, 0.0, 1.0, Core.NORM_MINMAX);
		float factor = (float)((1.0/360.0) * (180.0/255.0));
		var newAngle = new Mat();
		Core.multiply(angle, new Scalar(factor), newAngle);

		var _hsv = new ArrayList<Mat>();
		var hsv = new Mat();
		var hsv8 = new Mat();
		var bgr = new Mat();

		_hsv.add(newAngle);
		_hsv.add(Mat.ones(angle.size(), CvType.CV_32F));
		_hsv.add(magnNorm);
		Core.merge(_hsv, hsv);
		hsv.convertTo(hsv8, CvType.CV_8U, 255.0);
		Imgproc.cvtColor(hsv8, bgr, Imgproc.COLOR_HSV2BGR);

		HighGui.imshow("flow", bgr);

		var keyboard = HighGui.waitKey(0);
	}
}
