package org.chronopolis.tokenize.filter;

import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.TokenWorkSupervisor;

import java.util.function.Predicate;

/**
 * Simple predicate to test if a {@link ManifestEntry} should continue to be processed based on its
 * status in a {@link TokenWorkSupervisor}. If the {@link TokenWorkSupervisor} contains the Entry
 * already, then we do not want to continue processing it and want to return false. Otherwise we may
 * want to continue processing the Entry, so we return true.
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
