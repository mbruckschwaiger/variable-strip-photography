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
		var resultInfo = sc.calculateResultWidth();
		var resultWidth = resultInfo.width();
		var result = Mat.zeros(height, resultWidth, CvType.CV_8UC3);
		var sums = Mat.zeros(height, resultWidth, CvType.CV_64FC3);
		var coverage = new double[resultWidth];
		var pp = new ProgressPrinter("Stitching", sc.size());
		var frame = new Mat();
		var weighted = new Mat();
		var i = 0;
		var destinationX = resultInfo.startX();
		try {
			for (var s : sc) {
				pp.printProgress(++i);
				var start = Math.min(destinationX, destinationX + s.offset());
				destinationX += s.offset();
				var width = Math.abs(s.offset());
				if (width == 0) continue;
				vc.set(Videoio.CAP_PROP_POS_FRAMES, s.frameIndex());
				if (!vc.read(frame)) continue;
				var stripX = s.offset() < 0 ? s.stripPos() : s.stripPos() - width;
				for (var sourceX = (int)Math.floor(stripX); sourceX < Math.ceil(stripX + width); sourceX++) {
					var pixelLeft = start + (Math.max(sourceX, stripX) - stripX);
					var pixelRight = start + (Math.min(sourceX + 1.0, stripX + width) - stripX);
					var source = frame.submat(new Rect(sourceX, 0, 1, height));
					try {
						for (var x = Math.max(0, (int)Math.floor(pixelLeft)); x < Math.min(resultWidth, Math.ceil(pixelRight)); x++) {
							var weight = Math.min(x + 1.0, pixelRight) - Math.max(x, pixelLeft);
							if (weight <= 0) continue;
							var target = sums.col(x);
							try {
								source.convertTo(weighted, CvType.CV_64FC3, weight);
								org.opencv.core.Core.add(target, weighted, target);
								coverage[x] += weight;
							} finally {
								target.release();
							}
						}
					} finally {
						source.release();
					}
				}
			}
			for (var x = 0; x < resultWidth; x++) {
				if (coverage[x] == 0) continue;
				var source = sums.col(x);
				var target = result.col(x);
				try {
					source.convertTo(target, CvType.CV_8UC3, 1.0 / coverage[x]);
				} finally {
					source.release();
					target.release();
				}
			}
		} finally {
			frame.release();
			weighted.release();
			sums.release();
		}
		return result;
	}
}
