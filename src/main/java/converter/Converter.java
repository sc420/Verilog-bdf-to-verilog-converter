package converter;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Converter {
    private static final Pattern PATTERN_METADATA_STARTCHAR = Pattern.compile(
            "STARTCHAR (?<startCharNum>\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_METADATA_BBX = Pattern.compile(
            "BBX (?<width>\\d+) (?<height>\\d+) (?<offsetX>\\d+) (?<offsetY>\\d+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_METADATA_BITMAP = Pattern.compile(
            "BITMAP",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_BITMAP_PIXELS = Pattern.compile(
            "(?<row>[0-9A-F]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_METADATA_ENDCHAR = Pattern.compile(
            "ENDCHAR",
            Pattern.CASE_INSENSITIVE);

    // Files
    private final File input;
    private final File output;

    // Parsing
    private ConverterState state;
    private int currentStartChar;
    private int currentBBXWidth;
    private int currentBBXHeight;
    private List<BitSet> currentBitmap;
    private int parsedBitmapRowCount;

    // Parsed contents
    private final List<Character> characters;

    public Converter(final File input, final File output) {
        this.input = input;
        this.output = output;

        this.state = ConverterState.EXPECT_METADATA_START_CHAR;
        this.currentStartChar = 0;
        this.currentBBXWidth = 0;
        this.currentBBXHeight = 0;
        this.currentBitmap = new ArrayList<BitSet>();
        this.parsedBitmapRowCount = 0;

        this.characters = new ArrayList<Character>();
    }

    public void convert() throws IOException {
        final BufferedReader reader =
                new BufferedReader(new InputStreamReader(new FileInputStream(this.input), "UTF-8"));
        String line;

        // Parse from input file.
        while ((line = reader.readLine()) != null) {
            this.interpret(line);
        }

        reader.close();

        // Convert row major bitmap to column major bitmap.
        this.convertBitmapRowToColumnMajor();

        // Save to output file.
        this.save();
    }

    private void convertBitmapRowToColumnMajor() {
        for (final Character character : this.characters) {
            character.convertBitmapRowToColumnMajor();
        }
    }

    private void save() throws IOException {
        final BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(new FileOutputStream(this.output), "UTF-8"));

        writer.write('\ufeff');

        writer.write("{");
        writer.newLine();

        for (int i = 0; i < this.characters.size(); ++i) {
            final StringBuffer sb = new StringBuffer();

            final Character character = this.characters.get(i);
            final Dimension size = character.getSize();
            final String hexFormat = String.format("%%0%dX", size.height / 4 + (size.height % 4 != 0 ? 1 : 0));

            sb.append("   ");

            for (int x = 0; x < size.width; ++x) {
                final BitSet column = character.getColumn(x);
                final long decimalValue = getBitSetToLong(column);

                sb.append(size.height);
                sb.append("'h");
                sb.append(String.format(hexFormat, decimalValue));

                if (!(i == this.characters.size() - 1 && x == size.width - 1)) {
                    sb.append(", ");
                } else {
                    sb.append(" ");
                }
            }

            sb.append(String.format("// STARTCHAR: %d", character.getStartChar()));

            writer.write(sb.toString());
            writer.newLine();
        }

        writer.write("}");
        writer.newLine();

        writer.close();

        System.out.printf("Saved %d characters.%n", this.characters.size());
    }

    private void interpret(final String line) {
        switch (this.state) {
        case EXPECT_METADATA_START_CHAR: {
            if (this.tryMatchMetadataStartChar(line)) {
                this.state = ConverterState.EXPECT_METADATA_BBX;
            }
        }
            break;
        case EXPECT_METADATA_BBX: {
            if (this.tryMatchMetadataBBX(line)) {
                this.state = ConverterState.EXPECT_METADATA_BITMAP;
            }
        }
            break;
        case EXPECT_METADATA_BITMAP: {
            if (this.tryMatchMetadataBitmap(line)) {
                // Create new bitmap pixels.
                this.currentBitmap = new ArrayList<BitSet>();

                // Reset parsed bitmap row count.
                this.parsedBitmapRowCount = 0;

                this.state = ConverterState.EXPECT_BITMAP_PIXELS;
            }
        }
            break;
        case EXPECT_BITMAP_PIXELS: {
            if (this.tryMatchBitmapPixels(line)) {
                this.parsedBitmapRowCount += 1;

                if (this.parsedBitmapRowCount == this.currentBBXHeight) {
                    this.state = ConverterState.EXPECT_METADATA_ENDCHAR;
                }
            }
        }
            break;
        case EXPECT_METADATA_ENDCHAR: {
            if (this.tryMatchMetadataEndChar(line)) {
                // Save current character.
                final Character character = new Character(this.currentStartChar,
                        new Dimension(this.currentBBXWidth, this.currentBBXHeight), this.currentBitmap);

                this.characters.add(character);

                this.state = ConverterState.EXPECT_METADATA_START_CHAR;
            }
        }
            break;
        default: {
            throw new IllegalArgumentException("Unexpected state.");
        }
        }
    }

    private boolean tryMatchMetadataStartChar(final String line) {
        final Matcher matcher = PATTERN_METADATA_STARTCHAR.matcher(line);

        if (matcher.find()) {
            final String startChar = matcher.group("startCharNum");

            try {
                this.currentStartChar = Integer.parseInt(startChar);

                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean tryMatchMetadataBBX(final String line) {
        final Matcher matcher = PATTERN_METADATA_BBX.matcher(line);

        if (matcher.find()) {
            final String width = matcher.group("width");
            final String height = matcher.group("height");

            try {
                this.currentBBXWidth = Integer.parseInt(width);
                this.currentBBXHeight = Integer.parseInt(height);

                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean tryMatchMetadataBitmap(final String line) {
        final Matcher matcher = PATTERN_METADATA_BITMAP.matcher(line);

        return matcher.find();
    }

    private boolean tryMatchBitmapPixels(final String line) {
        final Matcher matcher = PATTERN_BITMAP_PIXELS.matcher(line);

        if (matcher.find()) {
            final String row = matcher.group("row");
            final BitSet rowPixels = new BitSet();

            try {
                for (int hexCharIndex = 0; hexCharIndex < row.length(); ++hexCharIndex) {
                    final String pixel = row.substring(hexCharIndex, hexCharIndex + 1);
                    final int decimalValue = Integer.parseInt(pixel, 16);
                    final BitSet bitSet = BitSet.valueOf(new long[] { decimalValue });

                    for (int bitIndex = 0; bitIndex < 4; ++bitIndex) {
                        rowPixels.set(hexCharIndex * 4 + 4 - 1 - bitIndex, bitSet.get(bitIndex));
                    }
                }

                this.currentBitmap.add(rowPixels);

                return true;
            } catch (final NumberFormatException e) {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean tryMatchMetadataEndChar(final String line) {
        final Matcher matcher = PATTERN_METADATA_ENDCHAR.matcher(line);

        return matcher.find();
    }

    private static long getBitSetToLong(final BitSet bitSet) {
        long value = 0L;

        for (int i = 0; i < bitSet.length(); ++i) {
            value += bitSet.get(i) ? 1L << i : 0L;
        }

        return value;
    }
}
