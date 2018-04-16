package org.chronopolis.tokenize.registrar;

import edu.umiacs.ace.ims.api.IMSUtil;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.AceTokenModel;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link TokenRegistrar} that polls a {@link TokenWorkSupervisor} for {@link ManifestEntry}s which
 * have {@link TokenResponse}s ready to upload to the Chronopolis Ingest Server
 *
 * @author shake
 */
public class HttpTokenRegistrar implements TokenRegistrar, Runnable {

    private final Logger log = LoggerFactory.getLogger(HttpTokenRegistrar.class);

    private final String imsHost;
    private final TokenService tokens;
    private final TokenWorkSupervisor supervisor;

    private final AtomicBoolean running = new AtomicBoolean(true);

    public HttpTokenRegistrar(TokenService tokens,
                              TokenWorkSupervisor supervisor,
                              AceConfiguration configuration) {
        this.tokens = tokens;
        this.supervisor = supervisor;
        this.imsHost = configuration.getIms().getEndpoint();
    }

    @Override
    public void run() {
        // These don't need to follow the same rules as the token request batch so... just do
        // our own thing
        final int size = 1000;
        final long timeout = 1000;
        final TimeUnit timeUnit = TimeUnit.MILLISECONDS;

        if (running.get()) {
            Map<ManifestEntry, TokenResponse> responses =
                    supervisor.tokenizedEntries(size, timeout, timeUnit);
            responses.forEach(this::register);
        }
    }

    public void close() {
        running.getAndSet(false);
    }

    @Override
    public void register(Map<ManifestEntry, TokenResponse> responseMap) {
        responseMap.forEach(this::register);
    }

    private void register(ManifestEntry entry, TokenResponse response) {
        if (!running.get()) {
            return;
        }

        String filename = getFilename(response);

        AceTokenModel model = new AceTokenModel()
                .setAlgorithm(response.getDigestService())
                .setFilename(filename)
                .setProof(IMSUtil.formatProof(response))
                .setImsHost(imsHost)
                .setImsService(response.getTokenClassName())
                .setRound(response.getRoundId())
                .setCreateDate(response.getTimestamp().toGregorianCalendar().toZonedDateTime());

        Call<AceTokenModel> call = tokens.createToken(entry.getBag().getId(), model);
        call.enqueue(new IngestCallback(entry));
    }

    private class IngestCallback implements Callback<AceTokenModel> {

        private final ManifestEntry entry;
        private final AtomicInteger tries;

        private IngestCallback(ManifestEntry entry) {
            this.entry = entry;
            this.tries = new AtomicInteger(0);
        }

        @Override
        public void onResponse(Call<AceTokenModel> call, Response<AceTokenModel> response) {
            if (response.isSuccessful() || response.code() == 409) {
                log.debug("[{}] Token Ingested into Chronopolis", entry.tokenName());
            } else {
                log.error("[{}] Token Not Ingested into Chronopolis", entry.tokenName());
                // do we want to retry here? depending on the code received?
            }

            supervisor.complete(entry);
        }

        @Override
        public void onFailure(Call<AceTokenModel> call, Throwable t) {
            log.error("[{}] Error communicating with Chronopolis", entry.tokenName(), t);

            // test this
            if (tries.incrementAndGet() < 3) {
                call.enqueue(this);
            } else {
                supervisor.retryRegister(entry);
            }
        }
    }
}
