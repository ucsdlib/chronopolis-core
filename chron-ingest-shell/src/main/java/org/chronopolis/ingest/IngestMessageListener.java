/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;

/**
 *
 * @author shake 
 */
public class IngestMessageListener extends ChronMessageListener {
	private ChronProcessor packageIngestStatusQueryProcessor;
	private ChronProcessor packageReadyProcessor;

	public IngestMessageListener(ChronProcessor packageIngestStatusQueryProcessor,
								 ChronProcessor packageReadyProcessor) {
		this.packageIngestStatusQueryProcessor = packageIngestStatusQueryProcessor;
		this.packageReadyProcessor = packageReadyProcessor;
	}

	@Override
	public ChronProcessor getProcessor(MessageType type) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}
	
}
