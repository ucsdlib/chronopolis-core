package org.chronopolis.ingest.controller;

import org.chronopolis.ingest.exception.BadRequestException;
import org.chronopolis.ingest.exception.ConflictException;
import org.chronopolis.ingest.exception.ForbiddenException;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.dao.StagingDao;
import org.chronopolis.ingest.support.FileSizeFormatter;
import org.chronopolis.ingest.support.Loggers;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.DataFile;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDataFile;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.create.StagingCreate;
import org.chronopolis.rest.models.enums.DataType;
import org.chronopolis.rest.models.enums.StorageUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.validation.Valid;
import java.security.Principal;
import java.util.Optional;

import static org.chronopolis.ingest.IngestController.hasRoleAdmin;

/**
 * Controller for Staging mappings
 * <p>
 * GET /bags/{id}/storage/
 * GET /bags/{id}/storage/{storageId}/activate
 * GET /bags/{id}/storage/add  => update the path?
 * POST /bags/{id}/storage/add => update the path?
 *
 * @author shake
 */
@Controller
public class StagingUIController {

    private final Logger access = LoggerFactory.getLogger(Loggers.ACCESS_LOG);
    private final Logger log = LoggerFactory.getLogger(StagingUIController.class);

    private final StagingDao dao;

    @Autowired
    public StagingUIController(StagingDao dao) {
        this.dao = dao;
    }

    /**
     * Invert the active flag for Storage in a Bag
     * <p>
     * Before 3.0 release - should this be a one way street? i.e. only allow true -> false
     *
     * @param principal the principal of the user
     * @param id        the id of the bag
     * @param storageId the id of the stagingStorage
     * @return a redirect to the "/bags/:id" template
     */
    @GetMapping("/bags/{id}/storage/{storageId}/activate")
    public String updateBagStorage(Principal principal,
                                   @PathVariable("id") Long id,
                                   @PathVariable("storageId") Long storageId)
            throws ForbiddenException {
        access.info("[GET /bags/{}/storage/{}/activate] - {}", id, storageId, principal.getName());

        Optional<StagingStorage> opt = dao.activeStorageForBag(id, StagingDao.DISCRIMINATOR_BAG);
        StagingStorage staging = opt.orElseThrow(() ->
                new NotFoundException("How about you request the proper resource"));

        StorageRegion region = staging.getRegion();
        String owner = region.getNode().getUsername();

        if (!hasRoleAdmin() && !owner.equalsIgnoreCase(principal.getName())) {
            throw new ForbiddenException("You shall not pass");
        }

        staging.setActive(!staging.isActive());
        dao.save(staging);
        return "redirect:/bags/" + id;
    }

    /**
     * Retrieve all staging storage resources for a {@link Bag}
     *
     * @param model     the model to add the storage attributes to
     * @param principal the security principal of the user
     * @param id        the id of the {@link Bag}
     * @return the "staging/all" template
     */
    @GetMapping("/bags/{id}/storage")
    public String getAllStorage(Model model, Principal principal, @PathVariable("id") Long id) {
        access.info("[GET /bags/{}/storage/add] - {}", id, principal.getName());

        Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(id));
        // for whatever reason we need to use get*Storage to retrieve the entities from the DB
        // maybe the lazy loading idk
        log.info("{} #storage = {}", bag.getName(), bag.getStorage().size());
        model.addAttribute("bag", bag);
        model.addAttribute("formatter", new FileSizeFormatter());
        return "staging/all";
    }

    /**
     * Retrieve the page for inputting form data to create a new StagingStorage entity for a Bag
     *
     * @param model         the view model
     * @param principal     the principal of the user
     * @param id            the id of the bag
     * @param stagingCreate the model with the form data
     * @return the create form
     */
    @GetMapping("/bags/{id}/storage/add")
    public String addStagingForm(Model model,
                                 Principal principal,
                                 @PathVariable("id") Long id,
                                 StagingCreate stagingCreate) {
        access.info("[GET /bags/{}/storage/add] - {}", id, principal.getName());

        Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(id));
        Optional<StagingStorage> stagingStorage =
                dao.activeStorageForBag(id, StagingDao.DISCRIMINATOR_BAG);
        model.addAttribute("bag", bag);
        model.addAttribute("conflict", stagingStorage.isPresent());
        model.addAttribute("regions", dao.findAll(QStorageRegion.storageRegion));
        model.addAttribute("storageUnits", StorageUnit.values());
        model.addAttribute("stagingCreate", stagingCreate);
        return "staging/create";
    }

    /**
     * Add a StagingStorage entity to a Bag
     * <p>
     * This requires the StagingCreate form data to be valid (if not, we return the create form
     * again), and ownership of the Bag/an admin user. We will also require that the StorageRegion
     * used is part of the same Node as the Bag, but right now we don't exactly have the means of
     * doing that
     * <p>
     * todo: update this for the world of files
     *
     * @param model         the model to pass to the view
     * @param principal     the principal of the user
     * @param id            the id of the Bag
     * @param stagingCreate the form data
     * @param bindingResult the validation data for the form
     * @return redirect to the Bag if successful
     *         or return the create form if there are errors
     *         or a 4xx page for any other type of errors
     * @throws ForbiddenException if the user is not allowed to modify the Bag
     */
    @PostMapping("/bags/{id}/storage/add")
    public String addStaging(Model model,
                             Principal principal,
                             @PathVariable("id") Long id,
                             @Valid StagingCreate stagingCreate,
                             BindingResult bindingResult) throws ForbiddenException {
        access.info("[POST /bags/{}/storage/add] - {}", id, principal.getName());
        if (bindingResult.hasErrors()) {
            model.addAttribute("regions", dao.findAll(QStorageRegion.storageRegion));
            model.addAttribute("storageUnits", StorageUnit.values());
            model.addAttribute("stagingCreate", stagingCreate);
            return "staging/create";
        }

        Bag bag = dao.findOne(QBag.bag, QBag.bag.id.eq(id));

        // not too crazy about the stream... but... it works
        Boolean activeExists = bag.getStorage().stream()
                .filter(s -> DataType.BAG == s.getRegion().getDataType())
                .anyMatch(StagingStorage::isActive);

        // retrieve the file to be used for validation
        DataFile validationFile = dao.findOne(QDataFile.dataFile,
                QDataFile.dataFile.filename.eq(stagingCreate.getValidationFile())
                        .and(QDataFile.dataFile.bag.eq(bag)));

        StorageRegion region = dao.findOne(QStorageRegion.storageRegion,
                QStorageRegion.storageRegion.id.eq(stagingCreate.getStorageRegion()));

        if (!hasRoleAdmin() && !bag.getCreator().equalsIgnoreCase(principal.getName())) {
            throw new ForbiddenException("User is not allowed to update this resource");
        } else if (activeExists) {
            throw new ConflictException("Resource already has active storage!");
        } else if (validationFile == null) {
            throw new BadRequestException("File for validation does not exist!");
        }

        double multiple = Math.pow(1000, stagingCreate.getStorageUnit().getPower());
        long size = Double.valueOf(stagingCreate.getSize() * multiple).longValue();

        StagingStorage storage = new StagingStorage();
        storage.setBag(bag);
        storage.setSize(size);
        storage.setActive(true);
        storage.setRegion(region);
        storage.setPath(stagingCreate.getLocation());
        storage.setTotalFiles(stagingCreate.getTotalFiles());
        storage.setFile(validationFile);
        bag.getStorage().add(storage);
        dao.save(bag);

        return "redirect:/bags/" + id;
    }

}
