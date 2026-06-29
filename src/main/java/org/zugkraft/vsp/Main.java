package org.zugkraft.vsp;

import org.apache.commons.cli.ParseException;

import java.io.IOException;

public class Main {
	static void main(String[] args) throws ParseException, IOException {
		var arguments = ArgumentParser.parseArguments(args);
		var vsp = new VariableStripPhoto();
		vsp.createStripPhoto(arguments.inputVideoName(), arguments.outputImagePath());
	}
}
