package org.zugkraft.vsp;

import org.apache.commons.cli.ParseException;
import org.zugkraft.vsp.method.VspMethodFactory;

import java.io.IOException;

public class Main {
	static void main(String[] args) throws ParseException, IOException {
		var arguments = ArgumentParser.parseArguments(args);
		var method = VspMethodFactory.createMethod(arguments);
		var vsp = new VariableStripPhoto(method);
		vsp.createStripPhoto(arguments.inputVideoName(), arguments.outputImagePath());
	}
}
