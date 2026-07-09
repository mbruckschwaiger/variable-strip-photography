package org.zugkraft.vsp.utils;

public class ProgressPrinter {

	private final int total;

	public ProgressPrinter(int total) {
		this.total = total;
	}

	public void printProgress(int current, double calculatedValue) {
		int width = 50;

		var progress = (double) current / total;
		var filled = (int) (progress * width);

		var bar = "[" + "=".repeat(filled) + " ".repeat(width - filled) + "]";

		System.out.printf(
				"\r%s %6.2f%% (%d/%d) : %6.2f",
				bar,
				progress * 100,
				current,
				total,
				calculatedValue
		);

		System.out.flush();
	}
}
