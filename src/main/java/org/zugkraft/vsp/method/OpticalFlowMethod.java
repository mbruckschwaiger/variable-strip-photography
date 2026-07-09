package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.zugkraft.vsp.utils.DebugHandler;
import org.zugkraft.vsp.utils.FlowVisualizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

public sealed abstract class OpticalFlowMethod implements VspMethod permits FarnebackOpticalFlowMethod {

	protected final DebugHandler debugHandler;
	protected Mat lastImage = null;

	protected abstract Mat prepareImage(Mat image);
	protected abstract Mat calculateFlow(Mat image);

	OpticalFlowMethod(DebugHandler debugHandler) {
		this.debugHandler = debugHandler;
	}

	@Override
	public OptionalInt getStripOffset(Mat image) {
		if (lastImage == null) {
			lastImage = prepareImage(image);
			return OptionalInt.empty();
		}

		var preparedImage = prepareImage(image);
		var flow = calculateFlow(preparedImage);
		var offset = estimateOffset(flow);
		var roundedOffset = Math.round(offset);

		debugHandler.addToLog(roundedOffset);

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

		Mat magnitude = new Mat();
		Mat angle = new Mat();

		Core.cartToPolar(flowX, flowY, magnitude, angle, true);

		var maskNoNoise180 = new Mat();
		var maskNoNoise0 = new Mat();
		Core.compare(flowX, Scalar.all(0.9), maskNoNoise0, Core.CMP_GT);
		Core.compare(flowX, Scalar.all(-0.9), maskNoNoise180, Core.CMP_LT);

		handleDebug(magnitude, angle, maskNoNoise0, maskNoNoise180);

		var size0 = Core.countNonZero(maskNoNoise0);
		var size180 = Core.countNonZero(maskNoNoise180);

		var minSize = lastImage.rows() * lastImage.cols() * 0.001;
		if (size0 < minSize && size180 < minSize)
			return 0;

		var maxValue = 200;
		Mat hist = new Mat();
		var mask = (size0 > size180) ? maskNoNoise0 : maskNoNoise180;
		Imgproc.calcHist(
				List.of(magnitude),
				new MatOfInt(0),
				mask,
				hist,
				new MatOfInt(maxValue),
				new MatOfFloat(0f, (float) maxValue)
		);

		var peak = -1;
		var peakValue = -1.0;
		for (int i = 0; i < maxValue; i++) {
			var count = hist.get(i, 0)[0];
			var weight = i*i / (double)maxValue;
			var weightedValue = count * weight;
			if (weightedValue > peakValue) {
				peak = i;
				peakValue = weightedValue;
			}
		}

		if (peak == -1)
			return 0;

		return ((size0 > size180) ? -1.0f : 1.0f) * (float)peak;
	}

	private void handleDebug(Mat magnitude, Mat angle, Mat maskAngle0, Mat maskAngle180) {
		if (!debugHandler.isActive())
			return;

		debugHandler.write(
				Map.of(
						"flow", FlowVisualizer.createFlowImage(magnitude, angle),
						"mag_0", FlowVisualizer.drawMagnitudeHistogram(maskAngle0, magnitude, 200, 200, true),
						"mag_180", FlowVisualizer.drawMagnitudeHistogram(maskAngle180, magnitude, 200, 200, true)
				)
		);

//		var angleImgFileName = fileBaseName + "_angle.png";
//		var anglePath = Path.of(debugOutputDirectory, angleImgFileName);
//		Imgcodecs.imwrite(anglePath.toString(), FlowHistogramVisualizer.drawAngleHistogram(mask, angle, 360, false));
	}
}
