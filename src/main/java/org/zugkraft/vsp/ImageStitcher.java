package org.zugkraft.vsp;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.zugkraft.vsp.utils.ProgressPrinter;

class ImageStitcher {

	public Mat stitch(VideoCapture vc, StripCollector sc) {
		var height = (int)vc.get(Videoio.CAP_PROP_FRAME_HEIGHT);
		var minX = sc.calculateMinX();
		var maxX = sc.calculateMaxX();
		var result = new Mat(height, maxX - minX, CvType.CV_8UC3);
		var pp = new ProgressPrinter("Stitching", sc.size());
		var i = 1;
		for (var s : sc) {
			pp.printProgress(i);
			var width = Math.abs(s.offset());
			var destinationX = s.offset() > 0 ? s.pos() - minX : s.pos() - minX - width;
			var destinationRoi = new Rect(destinationX, 0, width, height);
			var resultMat = result.submat(destinationRoi);
			vc.set(Videoio.CAP_PROP_POS_FRAMES, s.frameIndex());
			var frame = new Mat();
			vc.read(frame);
			var stripX = s.offset() < 0 ? s.stripPos() : s.stripPos() - width;
			var stripRoi = new Rect(stripX, 0, width, height);
			var strip = new Mat(frame, stripRoi);
			strip.copyTo(resultMat);
			i++;
		}
		return result;
	}
}
