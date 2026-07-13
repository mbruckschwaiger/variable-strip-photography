package org.zugkraft.vsp;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.Comparator;
import java.util.TreeSet;

class StripCollector extends TreeSet<Strip> {

	StripCollector() {
		super(Comparator.comparingInt(Strip::pos));
	}

	private int pos = 0;

	public void add(Mat image, int stripPos, int o) {
		if (o < 0) pos += o;
		add(new Strip(createStrip(image, stripPos, o), pos));
		if (o > 0) pos += o;
	}

	private Mat createStrip(Mat image, int stripPos, int offset) {
		return new Mat(image, new Rect(stripPos, 0, Math.abs(offset), image.height()));
	}
}
