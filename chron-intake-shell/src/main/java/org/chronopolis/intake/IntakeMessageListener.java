/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake;

import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;

/**
 *
 * @author shake 
 */
public class IntakeMessageListener extends ChronMessageListener {

	private ChronProcessor packageIngestCompleteProcessor;
	private ChronProcessor packageIngestStatusResponseProcessor;

	public IntakeMessageListener(ChronProcessor packageIngestCompleteProcessor,
								 ChronProcessor packageIngestStatusResponseProcessor) {
	
		this.packageIngestCompleteProcessor = packageIngestCompleteProcessor;
		this.packageIngestStatusResponseProcessor = packageIngestStatusResponseProcessor;
	}

	@Override
	public ChronProcessor getProcessor(MessageType type) {
		switch (type) {
			case PACKAGE_INGEST_COMPLETE:
				return packageIngestCompleteProcessor;
			case PACKAGE_INGEST_STATUS_RESPONSE:
				return packageIngestStatusResponseProcessor;
			default:
				throw new RuntimeException("Unexpected message type: " + type.name());

		}
	}

}
