package org.zugkraft.vsp.method;

import org.opencv.core.Mat;

public record SimpleMethod(Integer stripWidth) implements VspMethod {

	@Override
	public int getStripOffset(Mat image) {
		return stripWidth;
	}
}
