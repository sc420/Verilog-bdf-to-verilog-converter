package converter;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

public class Character {
    private final int startChar;
    private final Dimension size;
    private List<BitSet> bitmap;

    public Character(final int startChar, final Dimension size, final List<BitSet> bitmap) {
        this.startChar = startChar;
        this.size = new Dimension(size);
        this.bitmap = new ArrayList<BitSet>(bitmap);
    }

    public int getStartChar() {
        return this.startChar;
    }

    public Dimension getSize() {
        return this.size;
    }

    public BitSet getColumn(final int x) {
        return this.bitmap.get(x);
    }

    public void convertBitmapRowToColumnMajor() {
        final List<BitSet> columnMajorBitmap = new ArrayList<BitSet>();
        final boolean[][] buffer = new boolean[this.size.width][this.size.height];

        // Write row major bitmap to bitmap buffer.
        for (int y = 0; y < this.size.height; ++y) {
            final BitSet row = this.bitmap.get(y);

            for (int x = 0; x < this.size.width; ++x) {
                final boolean bitValue = row.get(x);

                buffer[x][y] = bitValue;
            }
        }

        // Write bitmap buffer to bitmap using column major style.
        for (int x = 0; x < this.size.width; ++x) {
            final BitSet column = new BitSet();

            for (int y = 0; y < this.size.height; ++y) {
                final boolean bitValue = buffer[x][y];

                column.set(y, bitValue);
            }

            columnMajorBitmap.add(column);
        }

        this.bitmap = columnMajorBitmap;
    }
}
