package at.pst.util.split;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "Splitter", mixinStandardHelpOptions = true, version = "1.0", description = "Split and merge files.")
public class Splitter {
	private static final int BLOCK = 8192;
	private boolean verbose = false;


	public static void main(String[] args) {
		int exitCode = new CommandLine(new Splitter()).execute(args);
		System.exit(exitCode);
	}

	@Command(name = "split", description = "Split a file.")
	private void split(
			@Option(names = {"-n", "--number"}, description = "number of files to generate in split (default: 2)", defaultValue = "2") int number,
			@Option(names = {"-o", "--output"}, description = "output filename pattern containing %d (default: out-%03d.bin)", defaultValue = "out-%03d.bin") String outPattern,
			@Option(names = {"-v", "--verbose"}, description = "verbose output (default: false)") boolean verbose,
			@Parameters(index = "0") String filename
			) throws IOException {
		this.verbose = verbose;
		log("Splitting file '%s' into %d files using filename pattern '%s'", filename, number, outPattern);

		BufferedInputStream in = null;
		BufferedOutputStream[] out = new BufferedOutputStream[number];
		try {
			in = new BufferedInputStream(new FileInputStream(filename));

			for(int i=0; i < number; i++) {
				String outName = String.format(outPattern, i+1);
				log("Opening file '%s' for writing.", outName);
				out[i] = new BufferedOutputStream(new FileOutputStream(outName));
			}

			int which = 0;

			int b;
			while(-1 < (b = in.read())) {
				out[which].write(b);
				which = which + 1;
				if(which >= number) {
					which = 0;
				}
			}
		}
		finally {
			if(in != null) {
				in.close();
			}
			for(int i=0; i < number; i++) {
				if(out[i] != null) {
					out[i].close();
				}
			}
		}
		log("Done.");
	}

	@Command(name = "merge", description = "Merge files.")
	private void merge(
			@Option(names = {"-o", "--output"}, description = "output filename (default: out.bin)", defaultValue = "out.bin") String output,
			@Option(names = {"-s", "--sort"}, description = "sort filenames (default: true)", negatable = true, defaultValue = "true", fallbackValue = "true") boolean sort,
			@Option(names = {"-v", "--verbose"}, description = "verbose output (default: false)") boolean verbose,
			@Parameters(index = "0..*") String[] filenames
			) throws IOException {
		this.verbose = verbose;
		if(sort) {
			filenames = Stream.of(filenames).sorted().toArray(String[]::new);
		}
		log("Mergings files %s %s into file '%s'", String.join(", ", filenames), sort ? "sorted by filename" : "in given order", output);

		BufferedInputStream[] in = new BufferedInputStream[filenames.length];
		BufferedOutputStream out = null;
		try {
			out = new BufferedOutputStream(new FileOutputStream(output));

			for(int i=0; i < filenames.length; i++) {
				log("Opening file '%s' for reading.", filenames[i]);
				in[i] = new BufferedInputStream(new FileInputStream(filenames[i]));
			}

			int which = 0;

			int done = 0;
			while(done < filenames.length) {
				int b = in[which].read();
				if(b < 0) {
					done++;
				}
				else {
					out.write(b);
					which = which + 1;
					if(which >= filenames.length) {
						which = 0;
					}
				}
			}
		}
		finally {
			if(out != null) {
				out.close();
			}
			for(int i=0; i < filenames.length; i++) {
				if(in[i] != null) {
					in[i].close();
				}
			}
		}
		log("Done.");
	}

	private void log(String format, Object...objects) {
		if(verbose) {
			System.out.format(format, objects);
			System.out.println();
		}
	}
}
