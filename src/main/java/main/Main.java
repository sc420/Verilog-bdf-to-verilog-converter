package main;

import converter.Converter;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import java.io.File;

/**
 * Convert row major BDF file to column major Verilog code.
 */
public class Main {
    public static void main(final String[] args) {
        final ArgumentParser parser = createParser();
        final Namespace res = tryGetParsedArguments(args, parser);

        if (res == null) {
            System.exit(1);
            return;
        }

        printParsedArguments(res);

        if (!tryConvert(res)) {
            System.exit(1);
            return;
        }
    }

    private static ArgumentParser createParser() {
        final ArgumentParser parser = ArgumentParsers.newArgumentParser("bdf_to_verilog")
                .description("Convert row major BDF file to column major Verilog code.");

        parser.addArgument("pathToOutput")
                .help("Path to output file.");
        parser.addArgument("pathToInput")
                .help("Path to input file.");

        return parser;
    }

    private static Namespace tryGetParsedArguments(final String[] args, final ArgumentParser parser) {
        Namespace res;

        try {
            res = parser.parseArgs(args);
        } catch (final ArgumentParserException e) {
            parser.handleError(e);
            return null;
        }

        return res;
    }

    private static void printParsedArguments(final Namespace res) {
        final String pathToOutput = res.getString("pathToOutput");
        final String pathToInput = res.getString("pathToInput");

        System.out.printf("pathToOutput: %s%n", pathToOutput);
        System.out.printf("pathToInput: %s%n", pathToInput);
    }

    private static boolean tryConvert(final Namespace res) {
        try {
            final Converter converter = createConverter(res);

            converter.convert();
        } catch (final Exception e) {
            System.err.println(e.getMessage());
            return false;
        }

        return true;
    }

    private static Converter createConverter(final Namespace res) throws Exception {
        final String pathToOutput = res.getString("pathToOutput");
        final String pathToInput = res.getString("pathToInput");

        final Converter waveConverter = new Converter(new File(pathToInput), new File(pathToOutput));

        return waveConverter;
    }
}
