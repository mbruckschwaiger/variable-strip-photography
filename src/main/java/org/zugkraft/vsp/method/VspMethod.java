package org.zugkraft.vsp.method;

import org.opencv.core.Mat;

public sealed interface VspMethod permits SimpleMethod {
	Mat getStrip(Mat image);
}
