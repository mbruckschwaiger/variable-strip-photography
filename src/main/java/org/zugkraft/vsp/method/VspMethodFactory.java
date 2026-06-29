package org.zugkraft.vsp.method;

import org.zugkraft.vsp.Arguments;

public class VspMethodFactory {
	public static VspMethod createMethod(Arguments arguments) {
		if (arguments.simpleStripWidth() != null) {
			return new SimpleMethod(arguments.simpleStripWidth());
		}

		throw new IllegalArgumentException("no parameter for specific stitching method present");
	}
}
