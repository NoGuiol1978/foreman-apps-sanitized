package mn.foreman.pickaxe.command;

import mn.foreman.api.model.CommandDone;
import mn.foreman.api.model.CommandStart;
import mn.foreman.api.model.CommandUpdate;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/** A callback for processing command completions. */
public class QueuedCompletionCallback
        implements CommandCompletionCallback {

    /** All of the commands that should be completed immediately. */
    private static final List<String> IMMEDIATE_COMMANDS = Collections.emptyList();

    /** The commands. */
    private final BlockingQueue<QueuedCommand> commands;

    /** The finalizer. */
    private final CommandFinalizer finalizer;

    /**
     * Constructor.
     *
     * @param commands  The commands.
     * @param finalizer The finalizer.
     */
    public QueuedCompletionCallback(
            final BlockingQueue<QueuedCommand> commands,
            final CommandFinalizer finalizer) {
        this.commands = commands;
        this.finalizer = finalizer;
    }

    @Override
    public void done(
            final String commandId,
            final CommandDone done) {
        final QueuedCommand queuedCommand =
                QueuedCommand
                        .builder()
                        .commandId(commandId)
                        .command(done)
                        .build();
        if (isImmediate(done.getCommand())) {
            this.finalizer.finish(
                    Collections.singletonList(
                            queuedCommand));
        } else {
            this.commands.add(queuedCommand);
        }
    }

    @Override
    public void start(
            final String commandId,
            final CommandStart start) {
        final QueuedCommand queuedCommand =
                QueuedCommand
                        .builder()
                        .commandId(commandId)
                        .command(start)
                        .build();
        if (isImmediate(start.command)) {
            this.finalizer.finish(
                    Collections.singletonList(
                            queuedCommand));
        } else {
            this.commands.add(queuedCommand);
        }
    }

    @Override
    public void update(
            final String commandId,
            final CommandUpdate update) {
        final QueuedCommand queuedCommand =
                QueuedCommand
                        .builder()
                        .commandId(commandId)
                        .command(update)
                        .build();
        if (isImmediate(update.getCommand())) {
            this.finalizer.finish(
                    Collections.singletonList(
                            queuedCommand));
        } else {
            this.commands.add(queuedCommand);
        }
    }

    /**
     * Returns whether or not the command should be completed immediately.
     *
     * @param command The command.
     *
     * @return Whether the command should be completed immediately.
     */
    private static boolean isImmediate(final String command) {
        return IMMEDIATE_COMMANDS.contains(command);
    }
}