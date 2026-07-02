package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgproc.Imgproc;
import org.zugkraft.vsp.StripInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public sealed abstract class OpticalFlowMethod implements VspMethod permits FarnebackOpticalFlowMethod {

	protected Mat lastImage = null;

	protected abstract Mat prepareImage(Mat image);
	protected abstract Mat calculateFlow(Mat image);

	@Override
	public StripInfo getStrip(Mat image) {
		if (lastImage == null) {
			lastImage = prepareImage(image);
			return null;
		}

		var preparedImage = prepareImage(image);
		var flow = calculateFlow(preparedImage);

		var offset = estimateOffset(flow);
		var newStrip = createStripInfo(image, offset);
		if (newStrip != null) {
			lastImage = preparedImage;
		}
		return newStrip;
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


	private StripInfo createStripInfo(Mat image, float offset) {
		var w = Math.abs(Math.round(offset));
		if (w == 0) {
			return null;
		}

		var center = image.width() / 2;
		var stripRoi = new Rect(center - w / 2, 0, w, image.height());
		var strip = new Mat(image, stripRoi);
		System.out.println(offset);
		return new StripInfo(strip, offset >= 0 ? 1 : -1);
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
		var scaledBgr = new Mat();
		Imgproc.resize(bgr, scaledBgr, new Size(), 0.5, 0.5);

		HighGui.imshow("flow", scaledBgr);

		var _ = HighGui.waitKey(0);
	}
}
