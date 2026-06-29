package org.zugkraft.vsp.method;

import org.opencv.core.Mat;
import org.opencv.core.Range;

public record SimpleMethod(Integer stripWidth) implements VspMethod {

	@Override
	public Mat getStrip(Mat image) {
		var center = image.width() / 2;
		return new Mat(image, new Range(0, image.height()), new Range(center-stripWidth/2, center + stripWidth));
	}
}
