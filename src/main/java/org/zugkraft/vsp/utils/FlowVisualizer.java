package org.zugkraft.vsp.utils;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;
import java.util.List;

public class FlowVisualizer {

	public static Mat createFlowImage(Mat magnitude, Mat angle) {

		Mat magNorm = new Mat();
		Core.normalize(magnitude, magNorm, 0, 255, Core.NORM_MINMAX);
		magNorm.convertTo(magNorm, CvType.CV_8UC1);

		Mat hue = new Mat();
		angle.convertTo(hue, CvType.CV_8UC1, 0.5);

		Mat saturation = new Mat(angle.size(), CvType.CV_8UC1, new Scalar(255));

		List<Mat> hsvChannels = List.of(hue, saturation, magNorm);

		Mat hsv = new Mat();
		Core.merge(hsvChannels, hsv);

		Mat bgr = new Mat();
		Imgproc.cvtColor(hsv, bgr, Imgproc.COLOR_HSV2BGR);

		return bgr;
	}

	public static Mat drawMagnitudeHistogram(Mat mask, Mat magnitude, int bins, double maxMagnitude, boolean logScale) {
		Mat hist = new Mat();

		Imgproc.calcHist(
				List.of(magnitude),
				new MatOfInt(0),
				mask,
				hist,
				new MatOfInt(bins),
				new MatOfFloat(0f, (float) maxMagnitude)
		);

		return drawHistogram(
				logScale,
				hist,
				bins,
				2000,
				800,
				"Magnitude",
				null
		);
	}

	public static Mat drawAngleHistogram(Mat mask, Mat angleDegrees, int bins, boolean logScale) {
		Mat hist = new Mat();

		Imgproc.calcHist(
				List.of(angleDegrees),
				new MatOfInt(0),
				mask,
				hist,
				new MatOfInt(bins),
				new MatOfFloat(0f, 360f)
		);

		List<Marker> markers = List.of(
				new Marker(0.0,   new Scalar(0, 255, 0), "0"),
				new Marker(180.0, new Scalar(0, 0, 255), "180")
		);

		return drawHistogram(
				logScale,
				hist,
				bins,
				2000,
				800,
				"Angle",
				markers
		);
	}

	private static Mat drawHistogram(
			boolean logScale,
			Mat hist,
			int bins,
			int width,
			int height,
			String title,
			List<Marker> markers
	) {
		int marginLeft = 50;
		int marginBottom = 30;
		int marginTop = 25;
		int plotWidth = width - marginLeft - 20;
		int plotHeight = height - marginTop - marginBottom;

		Mat image = new Mat(height, width, CvType.CV_8UC3, new Scalar(255, 255, 255));

		double[] logValues = new double[bins];
		double maxLog = 0.0;

		for (int i = 0; i < bins; i++) {
			double count = hist.get(i, 0)[0];
			double logCount = logScale ? Math.log10(count + 1.0) : count;

			logValues[i] = logCount;
			maxLog = Math.max(maxLog, logCount);
		}

		if (maxLog <= 0.0) {
			maxLog = 1.0;
		}

		// axes
		Imgproc.line(
				image,
				new Point(marginLeft, marginTop),
				new Point(marginLeft, height - marginBottom),
				new Scalar(0, 0, 0),
				1
		);

		Imgproc.line(
				image,
				new Point(marginLeft, height - marginBottom),
				new Point(width - 20, height - marginBottom),
				new Scalar(0, 0, 0),
				1
		);

		// histogram bars
		double binWidth = plotWidth / (double) bins;

		for (int i = 0; i < bins; i++) {
			int x1 = marginLeft + (int) Math.round(i * binWidth);
			int x2 = marginLeft + (int) Math.round((i + 1) * binWidth);

			int barHeight = (int) Math.round((logValues[i] / maxLog) * plotHeight);

			Point p1 = new Point(x1, height - marginBottom);
			Point p2 = new Point(x2, height - marginBottom - barHeight);

			Imgproc.rectangle(image, p1, p2, new Scalar(80, 80, 80), Imgproc.FILLED);
		}

		// optional angle markers
		if (markers != null) {
			for (Marker marker : markers) {
				double normalized = marker.value / 360.0;
				int x = marginLeft + (int) Math.round(normalized * plotWidth);

				Imgproc.line(
						image,
						new Point(x, marginTop),
						new Point(x, height - marginBottom),
						marker.color,
						2
				);

				Imgproc.putText(
						image,
						marker.label,
						new Point(x + 5, marginTop + 15),
						Imgproc.FONT_HERSHEY_SIMPLEX,
						0.45,
						marker.color,
						1
				);
			}
		}

		Imgproc.putText(
				image,
				title,
				new Point(10, 18),
				Imgproc.FONT_HERSHEY_SIMPLEX,
				0.75,
				new Scalar(0, 0, 0),
				1
		);

		var minMaxLoc = Core.minMaxLoc(hist);

		Imgproc.putText(
				image,
				String.format("max: %.2f @ %.2f", minMaxLoc.maxVal, minMaxLoc.maxLoc.y),
				new Point(500, 18),
				Imgproc.FONT_HERSHEY_SIMPLEX,
				0.75,
				new Scalar(0, 0, 0),
				1
		);

		return image;
	}

	public static List<Double> createMagnitudeHistogram(Mat mask, Mat magnitude, int bins, double maxMagnitude) {
		Mat hist = new Mat();

		Imgproc.calcHist(
				List.of(magnitude),
				new MatOfInt(0),
				mask,
				hist,
				new MatOfInt(bins),
				new MatOfFloat(0f, (float) maxMagnitude)
		);

		Mat hist64 = new Mat();
		hist.convertTo(hist64, CvType.CV_64F);
		double[] buf = new double[(int) hist64.total()];
		hist64.get(0, 0, buf);
		return Arrays.stream(buf).boxed().toList();
	}

	public static List<Double> createAngleHistogram(Mat mask, Mat angleDegrees) {
		Mat hist = new Mat();

		Imgproc.calcHist(
				List.of(angleDegrees),
				new MatOfInt(0),
				mask,
				hist,
				new MatOfInt(360),
				new MatOfFloat(0f, 360f)
		);
		Mat hist64 = new Mat();
		hist.convertTo(hist64, CvType.CV_64F);
		double[] buf = new double[(int) hist64.total()];
		hist64.get(0, 0, buf);
		return Arrays.stream(buf).boxed().toList();
	}

	private record Marker(double value, Scalar color, String label) {
	}
}
