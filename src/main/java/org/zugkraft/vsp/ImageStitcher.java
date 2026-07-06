package org.zugkraft.vsp;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.SortedSet;

class ImageStitcher {

	public Mat stitch(StripCollector stripCollector) {
		var firstColumn = stripCollector.getFirst();
		var firstStrip = firstColumn.strip();
		var minX = firstColumn.pos();
		var result = new Mat(firstStrip.height(), calculateWidth(stripCollector, minX), firstStrip.type());
		for (var s : stripCollector) {
			var strip = s.strip();
			var destinationRoi = new Rect(s.pos() - minX, 0, strip.width(), strip.height());
			var resultMat = result.submat(destinationRoi);
			strip.copyTo(resultMat);
		}
		return result;
	}

	private static int calculateWidth(SortedSet<Strip> columns, int minX) {
		var maxX = columns.stream()
				.mapToInt(c -> c.pos() + c.strip().width())
				.max().orElse(-1);

		return maxX - minX;
	}
}
