package org.zugkraft.vsp;

record Arguments(String inputVideoName,
                 String outputImagePath) {

	Arguments {
		if (inputVideoName == null || outputImagePath == null) {
			throw new IllegalArgumentException("input video and output image paths must be set");
		}
	}
}
