package org.zugkraft.vsp.method;

import org.opencv.core.Mat;

import java.util.OptionalDouble;

public sealed interface VspMethod permits FeatureTrackingMethod, OpticalFlowMethod, SimpleMethod, TemplateMethod {
	OptionalDouble getStripOffset(Mat image);

	default boolean isZero(double v) {
		return Math.abs(v) < 0.01;
	}
}
