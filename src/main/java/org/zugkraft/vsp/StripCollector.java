package org.zugkraft.vsp;

import java.util.ArrayList;

class StripCollector extends ArrayList<Strip> {

	public ResultInfo calculateResultWidth() {
		var left = 0;
		var right = 0;
		var sum = 0;
		for (var s : this) {
			sum += s.offset();
			left = Math.min(left, sum);
			right = Math.max(right, sum);
		}

		return new ResultInfo(right - left, -left);
	}
}
