package org.chronopolis.ingest.controller;

import com.querydsl.core.QueryModifiers;
import org.chronopolis.ingest.IngestController;
import org.chronopolis.ingest.repository.dao.BagFileDao;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.DataFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDataFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

/**
 * Request handler for operations on {@link org.chronopolis.rest.entities.DataFile}s
 *
 * @author shake
 */
@Controller
public class FileUIController extends IngestController {

    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);
    private final Logger log = LoggerFactory.getLogger(FileUIController.class);

    private final BagFileDao dao;

    @Autowired
    public FileUIController(BagFileDao dao) {
        this.dao = dao;
    }

    @GetMapping("/bags/{bag_id}/files")
    public String getFilesForBag(Model model,
                                 @PathVariable("bag_id") Long bagId) {
        log.info("[GET - /bags/{}/files]", bagId);
        Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(bagId));
        List<DataFile> files = dao.findAll(QDataFile.dataFile, QDataFile.dataFile.bag.eq(bag),
                QDataFile.dataFile.filename.asc(), new QueryModifiers(10L, 0L));
        model.addAttribute("bag", bag);
        model.addAttribute("files", files);
        return "files/bag_all";
    }

    @GetMapping("/bags/{bag_id}/files/{file_id}")
    public String getFileForBag(Model model,
                                @PathVariable("bag_id") Long bagId,
                                @PathVariable("file_id") Long fileId) {
        log.info("[GET - /bags/{}/files/{}]", bagId, fileId);
        final QBag qBag = QBag.bag;
        final QDataFile qDataFile = QDataFile.dataFile;

        Bag bag = dao.findOne(qBag, qBag.id.eq(bagId));
        DataFile file = dao.findOne(qDataFile, qDataFile.id.eq(fileId).and(qDataFile.bag.eq(bag)));

        model.addAttribute("bag", bag);
        model.addAttribute("file", file);

        return "files/bag_file";
    }

    @GetMapping("/bags/{bag_id}/files/create")
    public String getFileCreate(@PathVariable("bag_id") Long bagId) {
        return "files/create";
    }

    @PostMapping("/bags/{bag_id}/files")
    public String postFileCreate(@PathVariable("bag_id") Long bagId) {
        return "files/bag_file";
    }

    @GetMapping("/bags/{bag_id}/files/{file_id}/fixity")
    public String getFixityCreate(@PathVariable("bag_id") Long bagId,
                                  @PathVariable("file_id") Long fileId) {
        return "fixity/create";
    }

    @PostMapping("/bags/{bag_id}/files/{file_id}/fixity")
    public String postFixityCreate(@PathVariable("bag_id") Long bagId,
                                   @PathVariable("file_id") Long fileId) {
        return "files/bag_file";
    }

    @PostMapping("/bags/{bag_id}/files/{file_id}/fixity/{algorithm}")
    public String deleteFixity(@PathVariable("bag_id") Long bagId,
                               @PathVariable("file_id") Long fileId,
                               @PathVariable("algorithm") String algorithm) {
        return "files/bag_file";
    }

}
