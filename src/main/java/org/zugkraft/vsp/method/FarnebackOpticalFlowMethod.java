package org.zugkraft.vsp.method;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public final class FarnebackOpticalFlowMethod extends OpticalFlowMethod {

	private static final float SCALE = 16.0f;

	@Override
	protected Mat calculateFlow(Mat image) {
		Mat flow = new Mat(image.size(), CvType.CV_32FC2);
		Video.calcOpticalFlowFarneback(lastImage, image, flow, 0.5, 5, 15, 3, 5, 1.2, 0);
		//Video.calcOpticalFlowFarneback(lastGray, gray, flow, 0.9, 32, 45, 3, 5, 1.2, 0);
		Mat unscaledFlow = new Mat(flow.size(), flow.type());
		Core.multiply(flow, new Scalar(SCALE), unscaledFlow);
		return unscaledFlow;
	}

	protected Mat prepareImage(Mat image) {
		var grayImage = new Mat();
		Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
		var preparedImage = new Mat();
		var scaleFactor = 1.0 / SCALE;
		Imgproc.resize(grayImage, preparedImage, new Size(), scaleFactor, 1.0);
		return preparedImage;
	}
}
