package org.zugkraft.vsp.method;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

public final class FarnebackOpticalFlowMethod extends OpticalFlowMethod {

	private Mat lastFlow = null;

	FarnebackOpticalFlowMethod(String debugOutputDirectory) {
		super(debugOutputDirectory);
	}

	@Override
	protected Mat calculateFlow(Mat image) {
		Mat flow = lastFlow != null ? lastFlow : new Mat(image.size(), CvType.CV_32FC2);
		Video.calcOpticalFlowFarneback(lastImage, image, flow, 0.5, 5, 11, 2, 2, 1.0, lastFlow != null ? Video.OPTFLOW_USE_INITIAL_FLOW : 0);
		lastFlow = flow;
		return flow;
	}

	protected Mat prepareImage(Mat image) {
		var grayImage = new Mat();
		Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
		return grayImage;
	}
}
