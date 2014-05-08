/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.logger;

import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;

/**
 *
 * @author shake
 */
public class LogMessageListener extends ChronMessageListener {
    private ChronProcessor genericMessageProcessor;

    public LogMessageListener(ChronProcessor genericMessageProcessor) {
        this.genericMessageProcessor = genericMessageProcessor;
    }

    @Override
    public ChronProcessor getProcessor(MessageType type) {
        return genericMessageProcessor;
    }

}
