package org.zugkraft.vsp;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import org.zugkraft.vsp.method.VspMethod;
import org.zugkraft.vsp.utils.ProgressPrinter;

public class VariableStripPhoto {

	private final VspMethod method;
	private final StripCollector stripCollector = new StripCollector();
	private final ImageStitcher imageStitcher = new ImageStitcher();

	static {
		Loader.load(opencv_java.class);
	}

	public VariableStripPhoto(VspMethod method) {
		this.method = method;
	}

	public void createStripPhoto(FileHandler fh) {
		var vc = new VideoCapture(fh.inputVideo().toString());
		if (!vc.isOpened()) {
			throw new RuntimeException("could not open input video file");
		}

		var frameCount = (int)vc.get(Videoio.CAP_PROP_FRAME_COUNT);
		var frameWidth = (int)vc.get(Videoio.CAP_PROP_FRAME_WIDTH);

		var pp = new ProgressPrinter("Calculating strips", frameCount);
		var stripPos = frameWidth / 2;
		var image = new Mat();
		for (var i = 0; i < frameCount; i++) {
			vc.read(image);
			var offset = method.getStripOffset(image);
			int finalI = i;
			offset.ifPresent(o -> stripCollector.add(finalI, stripPos, o));
			pp.printProgress(i+1, (double)offset.orElse(0));
		}
		System.out.println();

		var result = imageStitcher.stitch(vc, stripCollector);

		Imgcodecs.imwrite(fh.resultImage().toString(), result);
	}
}
