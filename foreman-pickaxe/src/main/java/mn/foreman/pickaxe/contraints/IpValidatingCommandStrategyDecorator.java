package mn.foreman.pickaxe.contraints;

import mn.foreman.api.model.CommandDone;
import mn.foreman.api.model.CommandStart;
import mn.foreman.api.model.DoneStatus;
import mn.foreman.pickaxe.command.CommandCompletionCallback;
import mn.foreman.pickaxe.command.CommandStrategy;

import java.util.Map;

/**
 * A decorator that will validate that the command IP is allowed to be
 * examined.
 */
public class IpValidatingCommandStrategyDecorator
        implements CommandStrategy {

    /** The validator. */
    private final IpValidator ipValidator;

    /** The real strategy. */
    private final CommandStrategy real;

    /**
     * Constructor.
     *
     * @param real        The real strategy.
     * @param ipValidator The validator.
     */
    public IpValidatingCommandStrategyDecorator(
            final CommandStrategy real,
            final IpValidator ipValidator) {
        this.real = real;
        this.ipValidator = ipValidator;
    }

    @Override
    public void runCommand(
            final CommandStart start,
            final CommandCompletionCallback commandCompletionCallback,
            final CommandDone.CommandDoneBuilder builder) {
        final Map<String, Object> args = start.args;
        if (isAllowed(args, "ip") && isAllowed(args, "apiIp")) {
            this.real.runCommand(
                    start,
                    commandCompletionCallback,
                    builder);
        } else {
            // IP isn't allowed
            commandCompletionCallback.done(
                    start.id,
                    builder
                            .status(
                                    CommandDone.Status
                                            .builder()
                                            .type(DoneStatus.FAILED)
                                            .message("Denied by GUARDrail")
                                            .details("Denied by GUARDrail")
                                            .build())
                            .build());
        }
    }

    /**
     * Checks to see if the provided IP is allowed.
     *
     * @param args The arguments.
     * @param key  The key.
     *
     * @return Whether or not allowed.
     */
    private boolean isAllowed(
            final Map<String, Object> args,
            final String key) {
        boolean allowed = true;
        if (args != null && args.containsKey(key) && args.containsKey("mac")) {
            final Object ip = args.get(key);
            if (ip != null) {
                final String ipString = ip.toString();
                final String mac = args.getOrDefault("mac", "").toString();
                if (!ipString.isEmpty()) {
                    allowed = this.ipValidator.isAllowed(ipString, mac);
                }
            }
        }
        return allowed;
    }
}