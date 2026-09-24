package org.zugkraft.vsp.method;

import org.opencv.core.Mat;

import java.util.OptionalDouble;

public record SimpleMethod(Double stripWidth) implements VspMethod {

	@Override
	public OptionalDouble getStripOffset(Mat image) {
		return OptionalDouble.of(stripWidth);
	}
}
