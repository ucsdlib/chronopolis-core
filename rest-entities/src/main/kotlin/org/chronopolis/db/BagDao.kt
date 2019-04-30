package org.chronopolis.db

import org.chronopolis.db.binding.Pageable
import org.chronopolis.db.generated.Tables
import org.chronopolis.db.generated.tables.records.BagDistributionRecord
import org.chronopolis.db.generated.tables.records.BagRecord
import org.chronopolis.db.generated.tables.records.DepositorDistributionRecord
import org.chronopolis.db.generated.tables.records.DepositorRecord
import org.chronopolis.db.result.BagCreateResult
import org.chronopolis.db.result.CreateStatus
import org.chronopolis.rest.entities.BagDistributionStatus
import org.chronopolis.rest.models.create.BagCreate
import org.chronopolis.rest.models.enums.BagStatus
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.OrderField
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Optional

/**
 * Documentation for the [BagDao]
 *
 * @since 3.2.0
 * @author shake
 */
class BagDao(private val context: DSLContext) : Dao<BagRecord, Long> {

    private val log: Logger = LoggerFactory.getLogger(BagDao::class.java)

    override fun findOne(id: Long): Optional<BagRecord> {
        val bag = Tables.BAG
        return Optional.ofNullable(context.selectFrom(bag)
                .where(bag.ID.eq(id))
                .fetchOne())
    }

    /**
     * I'm curious if this could be pushed into the DAO and use the table as a parameter or
     * something. One thing which is tricky is the fetch into - if we provide a handler as an
     * abstract method which can be called from the extending class, it might work. Maybe not
     * necessary as this logic is pretty straightforward.
     */
    override fun findAll(conditions: Collection<Condition>,
                         order: Collection<OrderField<*>>,
                         limit: Limit): List<BagRecord> {
        val bag = Tables.BAG
        return context.selectFrom(bag)
                .where(conditions)
                .orderBy(order)
                .limit(limit.offset, limit.limit)
                .fetchInto(BagRecord::class.java)
    }

    override fun findAll(pageable: Pageable): List<BagRecord> {
        return findAll(pageable.conditions, pageable.order, pageable.getLimit())
    }

    /**
     * Processing for a [BagCreate] request.
     *
     * Precondition: the [BagCreate.depositor] exists
     * Precondition: the [BagCreate.name] is unique and not used
     *
     * If the [BagCreate.depositor] precondition fails, a [CreateStatus.BAD_REQUEST] is returned
     * with no [BagRecord]. If the [BagCreate.name] precondition fails, a [CreateStatus.CONFLICT] is
     * returned with no [BagRecord]. When both preconditions pass, a new [BagRecord] is made along
     * with a [BagDistributionRecord] for each location the package should be sent to (defined per
     * [DepositorRecord]).
     *
     * @param creator the username associated with the [Principal] who made the request
     * @param request the [BagCreate] request containing information about the package
     * @return [BagCreateResult] with information about what occured during processing
     * @since 3.2.0
     */
    fun processRequest(creator: String, request: BagCreate): BagCreateResult {
        val depositorT = Tables.DEPOSITOR
        // First search for the depositor
        // todo: handle exceptions
        val depositor = context.selectFrom(depositorT)
                .where(depositorT.NAMESPACE.eq(request.depositor))
                .fetchOne()

        if (depositor != null) {
            return createWithDepositor(creator, request, depositor)
        }

        log.warn("[{}] Depositor with namespace [{}] not found", request.name, request.depositor)
        return BagCreateResult(status = CreateStatus.BAD_REQUEST)
    }

    private fun createWithDepositor(creator: String,
                                    request: BagCreate,
                                    depositor: DepositorRecord): BagCreateResult {
        val bagT = Tables.BAG
        val count = context.selectCount()
                .from(bagT)
                .where(bagT.NAME.eq(request.name))
                .fetchOne(0, Int::class.java)

        return if (count == 0) {
            log.debug("[{}] Creating bag record", request.name)
            val record = context.newRecord(bagT)
            record.creator = creator
            record.size = request.size
            record.name = request.name
            record.depositorId = depositor.id
            record.totalFiles = request.totalFiles
            record.status = BagStatus.DEPOSITED.toString()
            record.store()
            // does this need to happen? we want to use the id later
            record.refresh()

            createDistributions(record, depositor)
            BagCreateResult(bag = record, status = CreateStatus.CREATED)
        } else {
            log.warn("Bag with name ${request.name} already exists")
            BagCreateResult(status = CreateStatus.CONFLICT)
        }
    }

    private fun createDistributions(record: BagRecord, depositor: DepositorRecord) {
        val bagDT = Tables.BAG_DISTRIBUTION
        // todo: pull depositor distributions along with depositor
        val distT = Tables.DEPOSITOR_DISTRIBUTION
        val distributions = context.selectFrom(distT)
                .where(distT.DEPOSITOR_ID.eq(depositor.id))
                .fetchInto(DepositorDistributionRecord::class.java)
        for (distribution in distributions) {
            log.debug("[{}] Creating distribution for []", record.name, distribution.nodeId)
            val distributionRecord: BagDistributionRecord = context.newRecord(bagDT)
            distributionRecord.bagId = record.id
            distributionRecord.status = BagDistributionStatus.DISTRIBUTE.toString()
            distributionRecord.nodeId = distribution.nodeId
            distributionRecord.store()
        }
    }

}