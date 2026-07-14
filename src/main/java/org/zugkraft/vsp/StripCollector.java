package org.zugkraft.vsp;

import java.util.ArrayList;

class StripCollector extends ArrayList<Strip> {

	private int pos = 0;

	public void add(int frameIndex, int stripPos, int o) {
		if (o < 0) pos += o;
		add(new Strip(frameIndex, stripPos, o, pos));
		if (o > 0) pos += o;
	}

	public int calculateMinX() {
		return this.stream()
				.mapToInt(s -> s.offset() > 0 ? s.pos() : s.pos() + s.offset())
				.min().orElse(-1);
	}

	public int calculateMaxX() {
		return this.stream()
				.mapToInt(s -> s.offset() > 0 ? s.pos() + s.offset() : s.pos())
				.max().orElse(-1);
	}
}
