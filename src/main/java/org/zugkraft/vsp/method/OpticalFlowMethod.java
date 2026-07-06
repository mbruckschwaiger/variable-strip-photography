package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;

public sealed abstract class OpticalFlowMethod implements VspMethod permits FarnebackOpticalFlowMethod {

	protected Mat lastImage = null;

	protected abstract Mat prepareImage(Mat image);
	protected abstract Mat calculateFlow(Mat image);

	@Override
	public OptionalInt getStripOffset(Mat image) {
		if (lastImage == null) {
			lastImage = prepareImage(image);
			return OptionalInt.empty();
		}

		var preparedImage = prepareImage(image);
		var flow = calculateFlow(preparedImage);

//		showDxHistogram(flow);
		visualize(flow);

		var offset = estimateOffset(flow);
		var roundedOffset = Math.round(offset);
		if (roundedOffset == 0) {
			return OptionalInt.empty();
		}
		lastImage = preparedImage;
		return OptionalInt.of(roundedOffset);
	}

	private float estimateOffset(Mat flow) {
		var flowParts = new ArrayList<Mat>(2);
		Core.split(flow, flowParts);
		var flowX = flowParts.get(0);
		var flowY = flowParts.get(1);

		float horizontalRatio = 3.0f;
		float epsilonSize = 1.0f;
		double minMagnitude = -200.0;
		double maxMagnitude = 200.0;

		var horizontalValues = new ArrayList<Float>();
		for (var y = 0; y < flow.rows(); y++) {
			for (var x = 0; x < flow.cols(); x++) {
				float dx = (float) flowX.get(y, x)[0];
				float dy = (float) flowY.get(y, x)[0];

				float absDx = Math.abs(dx);
				float absDy = Math.abs(dy);

				if (absDx > epsilonSize && dx > minMagnitude && dx < maxMagnitude && absDx > horizontalRatio * absDy) {
					horizontalValues.add(dx);
				}
			}
		}

//		if (horizontalValues.size() < flow.height() * flow.width() / 10) {
		if (horizontalValues.isEmpty()) {
			return 0;
		}

		var m = median(horizontalValues);
		var mad = mad(m, horizontalValues);

		var filteredHorizontalValues = horizontalValues.stream()
				.filter(dx -> Math.abs(dx - m) < 2.5 * 1.4826 * mad)
				.toList();

		if (filteredHorizontalValues.isEmpty()) {
			return 0;
		}
		var meanHorizontalValue = median(filteredHorizontalValues);
		return -meanHorizontalValue;
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

	private void visualize(Mat flow) {
		var angle = new Mat();
		var magnitude = new Mat();
		List<Mat> flows = new ArrayList<>();
		Core.split(flow, flows);
		Core.cartToPolar(flows.getFirst(), flows.getLast(), magnitude, angle);
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
		var scaledBgr = new Mat();
		Imgproc.resize(bgr, scaledBgr, new Size(), 0.5, 0.5);

		HighGui.imshow("flow", scaledBgr);

		var _ = HighGui.waitKey(0);
	}


	public static void showDxHistogram(Mat flow) {
		// flow should be CV_32FC2: [dx, dy]
		List<Mat> channels = new ArrayList<>();
		Core.split(flow, channels);

		Mat dx = channels.getFirst(); // horizontal flow values

//		var mm = Core.minMaxLoc(dx);
		int histBins = 40;
		float minDx = -20;
		float maxDx = 20;

		Mat hist = new Mat();

		Imgproc.calcHist(
				Collections.singletonList(dx),
				new MatOfInt(0),
				new Mat(),
				hist,
				new MatOfInt(histBins),
				new MatOfFloat(minDx, maxDx)
		);

		int histWidth = 800;
		int histHeight = 400;
		int binWidth = Math.round((float) histWidth / histBins);

		Mat histImage = Mat.zeros(histHeight, histWidth, CvType.CV_8UC3);

		Core.normalize(hist, hist, 0, histHeight, Core.NORM_MINMAX);

		for (int i = 0; i < histBins; i++) {
			double value = hist.get(i, 0)[0];

			Point p1 = new Point(i * binWidth, histHeight);
			Point p2 = new Point((i + 1) * binWidth, histHeight - value);

			Imgproc.rectangle(
					histImage,
					p1,
					p2,
					new Scalar(255, 255, 255),
					Imgproc.FILLED
			);
		}

		HighGui.imshow("dx histogram", histImage);
		HighGui.waitKey(1);
	}
}
