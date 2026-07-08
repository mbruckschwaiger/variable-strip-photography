package org.zugkraft.vsp;

public record Arguments(String inputVideoName,
						String outputImagePath,
						String debugOutputDirectory,
						Integer simpleStripWidth,
						Boolean useTemplateMethod,
						String opticalFlowMethod) {

	public Arguments {
		if (inputVideoName == null || outputImagePath == null) {
			throw new IllegalArgumentException("input video and output image paths must be set");
		}
	}
}
