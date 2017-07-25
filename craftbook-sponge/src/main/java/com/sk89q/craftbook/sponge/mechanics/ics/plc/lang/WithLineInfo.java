package com.sk89q.craftbook.sponge.mechanics.ics.plc.lang;

public class WithLineInfo {
    private final LineInfo[] lineInfo;
    private final String code;

    public WithLineInfo(LineInfo[] lineInfo, String code) {
        this.lineInfo = lineInfo;
        this.code = code;
    }

    public LineInfo[] getLineInfo() {
        return this.lineInfo;
    }

    public String getCode() {
        return this.code;
    }
}
