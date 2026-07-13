package org.zugkraft.vsp;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.zugkraft.vsp.utils.ProgressPrinter;

import java.util.SortedSet;

class ImageStitcher {

	public Mat stitch(VideoCapture vc, SortedSet<Strip> stripCollector) {
		var height = (int)vc.get(Videoio.CAP_PROP_FRAME_HEIGHT);
		var firstColumn = stripCollector.getFirst();
		var minX = firstColumn.pos();
		var result = new Mat(height, calculateWidth(stripCollector, minX), CvType.CV_8UC3);
		var pp = new ProgressPrinter("Stitching", stripCollector.size());
		var i = 1;
		for (var s : stripCollector) {
			pp.printProgress(i);
			var destinationRoi = new Rect(s.pos() - minX, 0, s.width(), height);
			var resultMat = result.submat(destinationRoi);
			vc.set(Videoio.CAP_PROP_POS_FRAMES, s.frameIndex());
			var frame = new Mat();
			vc.read(frame);
			var strip = new Mat(frame, new Rect(s.stripPos(), 0, s.width(), height));
			strip.copyTo(resultMat);
			i++;
		}
		return result;
	}

	private static int calculateWidth(SortedSet<Strip> columns, int minX) {
		var maxX = columns.stream()
				.mapToInt(s -> s.pos() + s.width())
				.max().orElse(-1);

		return maxX - minX;
	}
}
