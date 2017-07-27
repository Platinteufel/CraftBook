package com.sk89q.craftbook.sponge.mechanics.ics.plc;

import com.sk89q.craftbook.sponge.mechanics.ics.SerializedICData;
import com.sk89q.craftbook.sponge.mechanics.ics.factory.SerializedICFactory;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.persistence.InvalidDataException;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlcFactory<Lang extends PlcLanguage> extends SerializedICFactory<PlcIC<Lang>, PlcFactory.PlcStateData> {
    private Lang lang;

    public PlcFactory(Lang lang) {
        super(PlcStateData.class, 1);
        this.lang = lang;
    }

    @Override
    public PlcIC<Lang> createInstance(Location<World> location) {
        return new PlcIC<>(this, location, lang);
    }

    @Override
    protected Optional<PlcStateData> buildContent(DataView container) throws InvalidDataException {
        PlcStateData state = new PlcStateData();

        state.state = container.getBooleanList(DataQuery.of("State")).orElse(new ArrayList<>());
        state.languageName = container.getString(DataQuery.of("LanguageName")).orElse("Perlstone");
        state.codeString = container.getString(DataQuery.of("Code")).orElse("");
        state.error = container.getBoolean(DataQuery.of("Error")).orElse(false);
        state.errorCode = container.getString(DataQuery.of("ErrorCode")).orElse("");

        return Optional.of(state);
    }

    @Override
    public PlcStateData getData(PlcIC<Lang> ic) {
        return ic.state;
    }

    @Override
    public void setData(PlcIC<Lang> ic, PlcStateData data) {
        ic.state = data;
    }

    @Override
    public String[] getLineHelp() {
        return new String[] {"PLC ID", "Shared Access ID"};
    }

    @Override
    public String[][] getPinHelp() {
        return new String[0][];
    }

    public static class PlcStateData extends SerializedICData {
        public List<Boolean> state;
        public String languageName;
        public String codeString;
        public boolean error;
        public String errorCode;

        @Override
        public int getContentVersion() {
            return 1;
        }

        @Override
        public DataContainer toContainer() {
            return super.toContainer()
                    .set(DataQuery.of("State"), this.state)
                    .set(DataQuery.of("LanguageName"), this.languageName)
                    .set(DataQuery.of("Code"), this.codeString)
                    .set(DataQuery.of("Error"), this.error)
                    .set(DataQuery.of("ErrorCode"), this.errorCode);
        }
    }
}