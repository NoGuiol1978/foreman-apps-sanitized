package mn.foreman.pickaxe.util;

import mn.foreman.api.endpoints.pickaxe.Pickaxe;
import mn.foreman.model.Miner;
import mn.foreman.pickaxe.contraints.IpValidator;
import mn.foreman.pickaxe.contraints.ScopedMiner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Utilities for working with a {@link Miner}. */
public class MinerUtils {

    /**
     * Converts the provided config to a {@link Miner}.
     *
     * @param port         The port.
     * @param config       The config.
     * @param minerFactory The factory.
     * @param validator    The validator.
     *
     * @return The {@link Miner}.
     */
    public static Miner toMiner(
            final int port,
            final Pickaxe.MinerConfig config,
            final mn.foreman.model.MinerFactory minerFactory,
            final IpValidator validator) {
        final Map<String, Object> params = new HashMap<>();
        params.put(
                "apiIp",
                config.apiIp);
        params.put(
                "apiPort",
                Integer.toString(port));
        addParams(
                params,
                config.params);
        return new ScopedMiner(
                minerFactory.create(params),
                validator);
    }

    /**
     * Adds all of the params.
     *
     * @param dest   The destination.
     * @param params The params to add.
     */
    private static void addParams(
            final Map<String, Object> dest,
            final List<Pickaxe.MinerConfig.Param> params) {
        params.forEach(param -> dest.put(param.key, param.value));
    }
}
