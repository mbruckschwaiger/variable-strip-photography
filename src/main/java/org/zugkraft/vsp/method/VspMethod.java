package org.zugkraft.vsp.method;

import org.opencv.core.Mat;

public sealed interface VspMethod permits OpticalFlowMethod, SimpleMethod, TemplateMethod {
	int getStripOffset(Mat image);
}
