package org.chronopolis.tokenize.filter;

import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.TokenWorkSupervisor;

import java.util.function.Predicate;

/**
 * Simple predicate to test if a {@link TokenWorkSupervisor} has a {@link ManifestEntry}
 *
 * @author shake
 */
public class ProcessingFilter implements Predicate<ManifestEntry> {

    private final TokenWorkSupervisor supervisor;

    public ProcessingFilter(TokenWorkSupervisor supervisor) {
        this.supervisor = supervisor;
    }

    @Override
    public boolean test(ManifestEntry entry) {
        return !supervisor.isProcessing(entry);
    }
}
