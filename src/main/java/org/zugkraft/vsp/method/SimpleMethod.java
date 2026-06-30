package org.zugkraft.vsp.method;

import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.zugkraft.vsp.StripInfo;

public record SimpleMethod(Integer stripWidth) implements VspMethod {

	@Override
	public StripInfo getStrip(Mat image) {
		var center = image.width() / 2;
		return new StripInfo(new Mat(image, new Range(0, image.height()), new Range(center-stripWidth/2, center + stripWidth)), 1);
	}
}
