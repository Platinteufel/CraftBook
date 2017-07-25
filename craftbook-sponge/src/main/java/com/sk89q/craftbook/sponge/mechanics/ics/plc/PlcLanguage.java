package com.sk89q.craftbook.sponge.mechanics.ics.plc;

import com.sk89q.craftbook.sponge.mechanics.ics.IC;
import com.sk89q.craftbook.sponge.mechanics.ics.InvalidICException;
import com.sk89q.craftbook.sponge.mechanics.ics.plc.lang.WithLineInfo;

import java.util.List;

public interface PlcLanguage {
    String getName();

    List<Boolean> initState();

    WithLineInfo[] compile(String code) throws InvalidICException;

    boolean supports(String lang);

    String dumpState(List<Boolean> t);

    void execute(IC ic, List<Boolean> state, WithLineInfo[] code) throws PlcException;
}