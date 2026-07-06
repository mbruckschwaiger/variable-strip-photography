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
		add(new Strip(createStrip(image, stripPos, o), pos));
		pos += o;
	}

	private Mat createStrip(Mat image, int stripPos, int offset) {
		int left = Math.min(stripPos, stripPos + offset);
		return new Mat(image, new Rect(left, 0, Math.abs(offset), image.height()));
	}
}
