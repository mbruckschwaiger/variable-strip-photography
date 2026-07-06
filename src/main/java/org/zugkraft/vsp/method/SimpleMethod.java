package org.zugkraft.vsp.method;

import org.opencv.core.Mat;

import java.util.OptionalInt;

public record SimpleMethod(Integer stripWidth) implements VspMethod {

	@Override
	public OptionalInt getStripOffset(Mat image) {
		return OptionalInt.of(stripWidth);
	}
}
