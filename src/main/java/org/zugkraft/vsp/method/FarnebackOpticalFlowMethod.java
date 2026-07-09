package org.zugkraft.vsp.method;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;
import org.zugkraft.vsp.utils.DebugHandler;

public final class FarnebackOpticalFlowMethod extends OpticalFlowMethod {

	FarnebackOpticalFlowMethod(DebugHandler debugHandler) {
		super(debugHandler);
	}

	@Override
	protected Mat calculateFlow(Mat image) {
		Mat flow = new Mat(image.size(), CvType.CV_32FC2);
		Video.calcOpticalFlowFarneback(lastImage, image, flow, 0.5, 5, 11, 2, 2, 1.0, 0);
		return flow;
	}

	protected Mat prepareImage(Mat image) {
		var grayImage = new Mat();
		Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
		return grayImage;
	}
}
