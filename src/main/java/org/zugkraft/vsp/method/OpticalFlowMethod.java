package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.zugkraft.vsp.utils.DebugHandler;
import org.zugkraft.vsp.utils.FlowVisualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

public sealed abstract class OpticalFlowMethod implements VspMethod permits FarnebackOpticalFlowMethod, RaftOpticalFlowMethod {

	protected final DebugHandler debugHandler;
	protected Mat lastImage = null;
	private Rect centerRoi;

	protected abstract Mat prepareImage(Mat image);
	protected abstract Mat calculateFlow(Mat image);

	OpticalFlowMethod(DebugHandler debugHandler) {
		this.debugHandler = debugHandler;
	}

	@Override
	public OptionalDouble getStripOffset(Mat image) {
		if (lastImage == null) {
			lastImage = prepareImage(image);
			var edgeOffsetHorizontal = lastImage.width() / 50;
			var edgeOffsetVertical = lastImage.height() / 50;
			centerRoi = new Rect(
					edgeOffsetHorizontal,
					edgeOffsetVertical,
					lastImage.width() - (edgeOffsetHorizontal * 2),
					lastImage.height() - (edgeOffsetVertical * 2)
			);
			return OptionalDouble.empty();
		}

		var preparedImage = prepareImage(image);
		var flow = calculateFlow(preparedImage);
		var offset = estimateOffset(flow);

		debugHandler.addToLog(offset);

		if (isZero(offset)) {
			return OptionalDouble.empty();
		}
		lastImage = preparedImage;
		return OptionalDouble.of(offset);
	}

	private double estimateOffset(Mat flow) {
		var centerFlow = flow.submat(centerRoi);

		var flowParts = new ArrayList<Mat>(2);
		Core.split(centerFlow, flowParts);
		var flowX = flowParts.get(0);
		var flowY = flowParts.get(1);

		Mat magnitude = new Mat();
		Mat angle = new Mat();

		Core.cartToPolar(flowX, flowY, magnitude, angle, true);

		var maskAngle0 = new Mat();
		var maskAngle180 = new Mat();
		var maskMagnitude = new Mat();

		Core.compare(magnitude, Scalar.all(0.99), maskMagnitude, Core.CMP_GT);
		Core.inRange(angle, new Scalar(179), new Scalar(181), maskAngle180);
		var lowAngle0 = new Mat();
		var highAngle0 = new Mat();
		Core.inRange(angle, new Scalar(359), new Scalar(360), lowAngle0);
		Core.inRange(angle, new Scalar(0), new Scalar(1), highAngle0);
		Core.bitwise_or(lowAngle0, highAngle0, maskAngle0);

		var maskNoNoise180 = new Mat();
		var maskNoNoise0 = new Mat();
		Core.bitwise_and(maskMagnitude, maskAngle0, maskNoNoise0);
		Core.bitwise_and(maskMagnitude, maskAngle180, maskNoNoise180);

		handleDebug(magnitude, angle, maskNoNoise0, maskNoNoise180, maskMagnitude);

		var size0 = Core.countNonZero(maskNoNoise0);
		var size180 = Core.countNonZero(maskNoNoise180);

		var minSize = centerFlow.rows() * centerFlow.cols() * 0.001;
		if (size0 < minSize && size180 < minSize)
			return 0;

		var maxValue = 200;
		var accuracy = 100;
		var histSize = maxValue * accuracy;
		Mat hist = new Mat();
		var mask = (size0 > size180) ? maskNoNoise0 : maskNoNoise180;
		Imgproc.calcHist(
				List.of(magnitude),
				new MatOfInt(0),
				mask,
				hist,
				new MatOfInt(histSize),
				new MatOfFloat(0f, (float) maxValue)
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

		if (peak == -1)
			return 0;

		return ((size0 > size180) ? -1.0f : 1.0f) * (double)(peak) / accuracy;
	}

	private void handleDebug(Mat magnitude, Mat angle, Mat maskAngle0, Mat maskAngle180, Mat maskMagnitude) {
		if (!debugHandler.isActive())
			return;

		debugHandler.writeHistograms(
				Map.of(
						"mag_0", FlowVisualizer.createMagnitudeHistogram(maskAngle0, magnitude, 2000, 200),
						"mag_180", FlowVisualizer.createMagnitudeHistogram(maskAngle180, magnitude, 2000, 200),
						"angle", FlowVisualizer.createAngleHistogram(maskMagnitude, angle)
				)
		);
		debugHandler.write(
				Map.of(
						"flow", FlowVisualizer.createFlowImage(magnitude, angle)
				)
		);
	}
}
