package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.BFMatcher;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.ORB;
import org.opencv.imgproc.Imgproc;
import org.zugkraft.vsp.utils.DebugHandler;
import org.zugkraft.vsp.utils.FlowVisualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public final class FeatureTrackingMethod implements VspMethod {

	private final DebugHandler debugHandler;
	private ORB orb;
	private BFMatcher matcher;

	private Mat lastImage;
	private Mat lastDescriptors;
	private MatOfKeyPoint lastKeypoints;

	public FeatureTrackingMethod(DebugHandler dh) {
		this.debugHandler = dh;
	}

	@Override
	public OptionalDouble getStripOffset(Mat image) {
		var keypoints = new MatOfKeyPoint();
		var descriptors = new Mat();
		if (orb == null) {
			orb = ORB.create(1000);
			matcher = BFMatcher.create(Core.NORM_HAMMING, true);
		}

		orb.detectAndCompute(image, new Mat(), keypoints, descriptors);
		if (lastImage == null) {
			initLastData(image, keypoints, descriptors);
			return OptionalDouble.empty();
		}

		var matches = new MatOfDMatch();
		matcher.match(lastDescriptors, descriptors, matches);

		var kp1 = lastKeypoints.toList();
		var kp2 = keypoints.toList();

		var movingMatches = new ArrayList<DMatch>();
		var dxMat = Mat.zeros(image.size(), CvType.CV_32F);

		for (DMatch match : matches.toArray()) {
			var p1 = kp1.get(match.queryIdx).pt;
			var p2 = kp2.get(match.trainIdx).pt;

			double dx = p1.x - p2.x;
			double dy = p1.y - p2.y;

			if (Math.abs(dy) < 0.5 && Math.abs(dx) >= 1.0) {
				movingMatches.add(match);

				int x = (int) Math.round(p1.x);
				int y = (int) Math.round(p1.y);
				dxMat.put(y, x, dx);
			}
		}

		var offset = OptionalDouble.empty();

		if (!movingMatches.isEmpty()) {
			Mat mask = new Mat();

			Core.compare(
					dxMat,
					new Scalar(0),
					mask,
					Core.CMP_NE
			);

			var maxValue = 200;
			var minValue = -maxValue;
			var accuracy = 100;
			var histSize = (maxValue-minValue) * accuracy;
			var hist = new Mat();
			Imgproc.calcHist(
				List.of(dxMat),
				new MatOfInt(0),
				mask,
				hist,
				new MatOfInt(histSize),
				new MatOfFloat((float) -maxValue, (float) maxValue)
			);

			var peak = -1;
			var peakValue = -1.0;
			for (int i = 0; i < histSize; i++) {
				var count = hist.get(i, 0)[0];
				if (count > peakValue) {
					peak = i;
					peakValue = count;
				}
			}

			if (peak != -1 && peakValue > 2) {
				offset = OptionalDouble.of((double)peak / accuracy + minValue);
				debugHandler.addToLog(offset.getAsDouble(), peakValue);
			}


			MatOfDMatch goodMatchesMat = new MatOfDMatch();
			goodMatchesMat.fromList(movingMatches);

			Mat result = new Mat();

			Mat t1 = new Mat(), t2 = new Mat();
			Core.transpose(lastImage, t1);
			Core.transpose(image, t2);

			Features2d.drawMatches(t1, swapXY(lastKeypoints), t2, swapXY(keypoints), goodMatchesMat, result,
					2, new Scalar(255.0, 0.0, 0.0), new Scalar(0.0, 0.0, 255.0), new MatOfByte(),
					Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS
			);

			debugHandler.writeHistograms(Map.of("hist", FlowVisualizer.createMagnitudeHistogram(mask, dxMat, 2000, 200)));
			debugHandler.write(Map.of("FM", result));
		}

		initLastData(image, keypoints, descriptors);
		return offset;
	}

	private static MatOfKeyPoint swapXY(MatOfKeyPoint kps) {
		var src = kps.toArray();
		var dst = new KeyPoint[src.length];
		for (var i = 0; i < src.length; i++) {
			var k = src[i];
			dst[i] = new KeyPoint((float) k.pt.y, (float) k.pt.x, k.size,
					k.angle, k.response, k.octave, k.class_id);
		}
		return new MatOfKeyPoint(dst);
	}

	private void initLastData(Mat image, MatOfKeyPoint keypoints, Mat descriptors) {
		lastImage = image.clone();
		lastDescriptors = descriptors.clone();
		lastKeypoints = keypoints;
	}
}
