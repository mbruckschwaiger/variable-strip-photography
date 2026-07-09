package org.zugkraft.vsp.method;

import org.zugkraft.vsp.FileHandler;

import java.util.Properties;

public class VspMethodFactory {
	public static VspMethod createMethod(Properties properties, FileHandler fh) {
		var method = properties.getProperty("vsp.method");

		if ("simple".equalsIgnoreCase(method)) {
			var simpleWidth = properties.getProperty("vsp.simple.width");
			var simpleWidthInt = Integer.parseInt(simpleWidth);
			return new SimpleMethod(simpleWidthInt);
		}
		if ("template".equalsIgnoreCase(method)) {
			return new TemplateMethod();
		}
		if ("flow".equalsIgnoreCase(method)) {
			var flowAlgorithm = properties.getProperty("vsp.flow.algorithm");

			if ("farneback".equalsIgnoreCase(flowAlgorithm)) {
				return new FarnebackOpticalFlowMethod(fh.debugDirectory().toString());
			}

			throw new IllegalArgumentException(String.format("unknown vsp.flow.algoritm '%s'", flowAlgorithm));
		}

		throw new IllegalArgumentException(String.format("unknown vsp.method '%s'", method));
	}
}
