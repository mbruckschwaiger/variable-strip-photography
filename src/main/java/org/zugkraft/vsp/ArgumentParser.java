package org.zugkraft.vsp;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ArgumentParser {
	static Arguments parseArguments(String[] args) throws ParseException, IOException {
		var options = createOptions();
		var parser = new DefaultParser();
		var line = parser.parse(options, args);
		if (line.getOptions().length == 0 ||
			line.hasOption("h") || line.hasOption("?")) {
			var helpFormatter = HelpFormatter.builder().setShowSince(false).get();
			helpFormatter.printOptions(options);
			System.exit(1);

		}
		String input = line.getParsedOptionValue("i");
		String output = line.getParsedOptionValue("o");
		Integer simpleStripWidth = line.getParsedOptionValue("w");
		var useTemplateMethod = line.hasOption("t");
		String opticalFlowMethod = line.hasOption("f") ? line.getParsedOptionValue("f") : "";
		return new Arguments(input, output, simpleStripWidth, useTemplateMethod, opticalFlowMethod);
	}

	private static Options createOptions() {
		var optionList = createOptionList();
		var options = new Options();
		for (var o : optionList) {
			options.addOption(o);
		}
		return options;
	}

	private static List<Option> createOptionList() {
		var optionList = new ArrayList<Option>();
		optionList.add(new Option("h", "help", false, "print this message"));
		optionList.add(new Option("?", false, "print this message"));
		optionList.add(Option.builder("i")
				.longOpt("input")
				.desc("input video file")
				.hasArg()
				.type(String.class)
				.get());
		optionList.add(Option.builder("o")
				.longOpt("output")
				.desc("output image file")
				.hasArg()
				.type(String.class)
				.get());
		optionList.add(Option.builder("w")
				.longOpt("simple-strip-width")
				.desc("strip width for simple strip stitching")
				.hasArg()
				.type(Integer.class)
				.get());
		optionList.add(Option.builder("t")
				.longOpt("template")
				.desc("strip estimation with template matching")
				.hasArg(false)
				.get());
		optionList.add(Option.builder("f")
				.longOpt("optical-flow")
				.desc("strip estimation with optical flow")
				.hasArg()
				.type(String.class)
				.get());

		return optionList;
	}
}
