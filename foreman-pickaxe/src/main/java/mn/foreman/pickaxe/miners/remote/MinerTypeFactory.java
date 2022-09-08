package mn.foreman.pickaxe.miners.remote;

import mn.foreman.antminer.AntminerFactory;
import mn.foreman.api.model.ApiType;
import mn.foreman.model.ApplicationConfiguration;
import mn.foreman.model.MinerFactory;
import mn.foreman.whatsminer.WhatsminerFactory;

/** A factory for creating {@link MinerFactory factories} from configurations. */
public class MinerTypeFactory {

    /**
     * Creates a factory for the provided configuration.
     *
     * @param apiType       The API type.
     * @param port          The port.
     * @param configuration The configuration.
     *
     * @return The factory.
     */
    public static MinerFactory toFactory(
            final ApiType apiType,
            final int port,
            final ApplicationConfiguration configuration) {
        MinerFactory minerFactory = null;
        switch (apiType) {
            case ANTMINER_HS_API:
                minerFactory =
                        new AntminerFactory(
                                0.000000001,
                                configuration);
                break;
            case ANTMINER_MHS_API:
                minerFactory =
                        new AntminerFactory(
                                0.001,
                                configuration);
                break;
            case ANTMINER_GHS_API:
                minerFactory =
                        new AntminerFactory(
                                1,
                                configuration);
                break;
            case ANTMINER_KHS_API:
                minerFactory =
                        new AntminerFactory(
                                0.000001,
                                configuration);
                break;
            case WHATSMINER_API:
                minerFactory = new WhatsminerFactory(configuration);
                break;
        }
        return minerFactory;
    }
}
