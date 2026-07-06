package org.zugkraft.vsp.method;

import org.opencv.core.Mat;

import java.util.OptionalInt;

public sealed interface VspMethod permits OpticalFlowMethod, SimpleMethod, TemplateMethod {
	OptionalInt getStripOffset(Mat image);
}
