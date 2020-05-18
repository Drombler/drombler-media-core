package org.drombler.event.core;

import org.softsmithy.lib.text.FormatException;

import java.io.IOException;

/**
 * @author Florian
 */

// not used yet
public class InfiniteEventDuration implements EventDuration {
    private static final InfiniteEventDuration INSTANCE = new InfiniteEventDuration();

    private InfiniteEventDuration() {
    }

    private static final InfiniteEventDuration getInstance() {
        return INSTANCE;
    }

    @Override
    public Appendable formatDirName(Appendable appendable) throws FormatException {
        try {
            return appendable.append("infinite");
        } catch (IOException e) {
            throw new FormatException(e.getMessage(), e);
        }
    }
}
