package org.chronopolis.amqp;

import junit.framework.Assert;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;

public class TestProcessor implements ChronProcessor {
    private ChronMessage expected;

    public void setExpected(ChronMessage expected) {
        this.expected = expected;
    }

    @Override
    public void process(ChronMessage chronMessage) {
        Assert.assertNotNull(expected);

        Assert.assertTrue("Bodies", expected.getChronBody().equals(chronMessage.getChronBody()));
        Assert.assertTrue(expected.toString()+  " does not equal " + chronMessage.toString(), expected.equals(chronMessage));

        //expected = null;
    }
}
