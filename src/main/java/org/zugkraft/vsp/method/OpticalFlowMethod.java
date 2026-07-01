package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.zugkraft.vsp.StripInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class OpticalFlowMethod implements VspMethod {

	private Mat lastGray = null;
	private Float lastMeanHorizontalValue = null;

	@Override
	public StripInfo getStrip(Mat image) {
		if (lastGray == null) {
			lastGray = prepareImage(image);
			return null;
		}

		var gray = prepareImage(image);

		Mat flow = new Mat(gray.size(), CvType.CV_32FC2);
		Video.calcOpticalFlowFarneback(lastGray, gray, flow, 0.5, 3, 15, 3, 5, 1.2, 0);

		var flowParts = new ArrayList<Mat>(2);
		Core.split(flow, flowParts);
		var flowX = flowParts.get(0);
		var flowY = flowParts.get(1);

		float horizontalRatio = 3.0f;
		float epsilonSize = 1.0f;
		double minMagnitude = lastMeanHorizontalValue != null ? lastMeanHorizontalValue - 20.0 : -200.0;
		double maxMagnitude = lastMeanHorizontalValue != null ? lastMeanHorizontalValue + 20.0 : 200.0;

		var horizontalValues = new ArrayList<Float>();
		for (var y = 0; y < flow.rows(); y++) {
			for (var x = 0; x < flow.cols(); x++) {
				float dx = (float)flowX.get(y, x)[0];
				float dy = (float)flowY.get(y, x)[0];

				float absDx = Math.abs(dx);
				float absDy = Math.abs(dy);

				if (absDx > epsilonSize && dx > minMagnitude && dx < maxMagnitude && absDx > horizontalRatio * absDy) {
					horizontalValues.add(dx);
				}
			}
		}

//		var weightedValues = new ArrayList<WeightedValue>();
//
//		for (var i = 0; i < horizontalValues.size(); i++) {
//			weightedValues.add(new WeightedValue(i, horizontalValues.get(i)));
//		}

//		var m = weightedMedian(weightedValues);
		if (horizontalValues.isEmpty()) {
			return null;
		}
		var m = median(horizontalValues);
		var mad = mad(m, horizontalValues);

		var filteredHorizontalValues = horizontalValues.stream()
				.filter(dx -> Math.abs(dx - m) < 2.5 * 1.4826 * mad)
				.toList();

		if (filteredHorizontalValues.isEmpty()) {
			return null;
		}
		var meanHorizontalValue = median(filteredHorizontalValues);
		var offset = -meanHorizontalValue;

//		System.out.println(filteredHorizontalValues);
		System.out.println(m + " , " + mad + " , " + offset);

//		var magnitude = new Mat();
//		var angle = new Mat();
//		Core.cartToPolar(flowX, flowY, magnitude, angle, true);
//		visualize(angle, magnitude);

		var w = Math.abs(Math.round(offset));
		if (w == 0) {
			return null;
		}
		var center = image.width() / 2;
		var stripRoi = new Rect(center - w / 2, 0, w, image.height());
		var strip = new Mat(image, stripRoi);
		System.out.println(offset);
		lastGray = gray;
		lastMeanHorizontalValue = meanHorizontalValue;
		return new StripInfo(strip, offset >= 0 ? 1 : -1);
	}

	private Mat prepareImage(Mat image) {
		var grayImage = new Mat();
		Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
		var preparedImage = new Mat();
		Imgproc.GaussianBlur(grayImage, preparedImage, new Size(11, 11), 0, 0);
		return preparedImage;
	}

	private float mad(float m, List<Float> valueList) {
		var diffList = valueList.stream().map(dx -> Math.abs(dx - m)).toList();
		return median(diffList);
	}

	private float mean(List<Float> valueList) {
		var sum = valueList.stream().mapToDouble(Float::doubleValue).sum();
		return (float)(sum / valueList.size());
	}

	private float median(List<Float> valueList) {
		var sorted = new ArrayList<>(valueList);
		Collections.sort(sorted);

		int n = sorted.size();
		if ((n & 1) == 1) {
			return sorted.get(n / 2);
		}
		return (sorted.get(n / 2 - 1) + sorted.get(n / 2)) * 0.5f;
	}

	record WeightedValue(float value, float weight) {}

	private float weightedMedian(List<WeightedValue> values) {
		List<WeightedValue> sorted = new ArrayList<>(values);
		sorted.sort(Comparator.comparingDouble(WeightedValue::value));

		double totalWeight = sorted.stream()
				.mapToDouble(WeightedValue::weight)
				.sum();

		double halfWeight = totalWeight * 0.5;
		double cumulative = 0.0;

		for (WeightedValue v : sorted) {
			cumulative += v.weight();
			if (cumulative >= halfWeight) {
				return v.value();
			}
		}

		return sorted.getLast().value();
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
