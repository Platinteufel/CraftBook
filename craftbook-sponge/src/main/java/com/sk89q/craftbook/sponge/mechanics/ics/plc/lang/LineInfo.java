package com.sk89q.craftbook.sponge.mechanics.ics.plc.lang;

public class LineInfo {
    private final int line;
    private final int column;

    public LineInfo(int line, int column) {
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return this.line;
    }

    public int getColumn() {
        return this.column;
    }
}
