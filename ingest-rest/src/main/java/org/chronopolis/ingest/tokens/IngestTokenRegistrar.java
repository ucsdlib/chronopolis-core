package org.chronopolis.ingest.tokens;

import edu.umiacs.ace.ims.api.IMSUtil;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.chronopolis.tokenize.registrar.TokenRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Date;
import java.time.Instant;
import java.util.Map;

/**
 * Registrar which loads Tokens directly into the DB
 *
 * @author shake
 */
public class IngestTokenRegistrar implements TokenRegistrar {

    private final Logger log = LoggerFactory.getLogger(IngestTokenRegistrar.class);
    private static final String IMS_HOST = "ims.umiacs.umd.edu";

    private final PagedDAO dao;
    private final TokenWorkSupervisor supervisor;

    public IngestTokenRegistrar(PagedDAO dao, TokenWorkSupervisor supervisor) {
        this.dao = dao;
        this.supervisor = supervisor;
    }

    @Override
    public void register(Map<ManifestEntry, TokenResponse> tokenResponseMap) {
        tokenResponseMap.forEach((entry, response) -> {
            Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(entry.getBag().getId()));
            Instant responseInstant = response.getTimestamp().toGregorianCalendar().toInstant();

            String filename = getFilename(response);
            java.util.Date create = Date.from(responseInstant);
            AceToken token = new AceToken(bag, create, filename,
                    IMSUtil.formatProof(response),
                    IMS_HOST,
                    response.getDigestService(),
                    response.getDigestProvider(),
                    response.getRoundId());
            try {
                // probably want a response from our DAO instead to verify that the entity
                // is persisted
                dao.save(token);
            } catch (Exception e) {
                log.error("[{}] Unexpected exception when saving token!", response.getName(), e);
            } finally {
                supervisor.complete(entry);
            }
        });
    }
}
