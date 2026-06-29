package org.zugkraft.vsp.method;

import org.opencv.core.Mat;
import org.zugkraft.vsp.StripInfo;

public sealed interface VspMethod permits SimpleMethod, TemplateMethod {
	StripInfo getStrip(Mat lastImage, Mat image);
}
