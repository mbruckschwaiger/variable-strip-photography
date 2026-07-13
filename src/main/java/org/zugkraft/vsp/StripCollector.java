package org.zugkraft.vsp;

import java.util.Comparator;
import java.util.TreeSet;

class StripCollector extends TreeSet<Strip> {

	StripCollector() {
		super(Comparator.comparingInt(Strip::pos));
	}

	private int pos = 0;

	public void add(int frameIndex, int stripPos, int o) {
		if (o < 0) pos += o;
		add(new Strip(frameIndex, stripPos, Math.abs(o), pos));
		if (o > 0) pos += o;
	}
}
