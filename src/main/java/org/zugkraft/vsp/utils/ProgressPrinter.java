package org.zugkraft.vsp.utils;

import java.time.Duration;

public class ProgressPrinter {

	private final String title;
	private final int total;
	private final long startNano;

	public ProgressPrinter(String title, int total) {
		this.title = title;
		this.total = total;
		this.startNano = System.nanoTime();
	}

	public void printProgress(int current) {
		printProgress(current, null);
	}

	public void printProgress(int current, Double calculatedValue) {
		var width = 50;

		var progress = (double) current / total;
		var filled = (int) (progress * width);

		var bar = "[" + "=".repeat(filled) + " ".repeat(width - filled) + "]";

		System.out.printf(
				"\r%20s: (%s) %s %6.2f%% (%d/%d)" + (calculatedValue == null ? "" : " : %6.2f"),
				title,
				format(remaining(progress)),
				bar,
				progress * 100,
				current,
				total,
				calculatedValue
		);

		System.out.flush();
	}

	private Duration remaining(double progress) {
		var elapsedNanos = System.nanoTime() - startNano;

		var estimatedTotalNanos = (long)(elapsedNanos / progress);
		var remainingNanos = Math.max(0, estimatedTotalNanos - elapsedNanos);
		return Duration.ofNanos(remainingNanos);
	}

	private static String format(Duration d) {
		var seconds = d.toSeconds();
		long h = seconds / 3600;
		long m = (seconds % 3600) / 60;
		long s = seconds % 60;

		return String.format("%d:%02d:%02d", h, m, s);
	}
}
