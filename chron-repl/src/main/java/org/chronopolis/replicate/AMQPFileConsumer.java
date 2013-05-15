/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.chronopolis.replicate;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import org.json.simple.JSONObject;
import org.apache.log4j.Logger;
import org.chronopolis.messaging.base.ChronBody;

/**
 *
 * @author shake
 */
public class AMQPFileConsumer extends Thread {
    private static final String EXCHANGE = "chronopolis-control";
    private static final String VHOST = "chronopolis";
    private static final String bindingKey = "chron.distribute.transfer";
    private boolean RUN = true;

    private static final Logger LOG = Logger.getLogger(AMQPFileConsumer.class);
    private int blockSize = 65536;

    public AMQPFileConsumer() {
    }

    public void consume() throws IOException, InterruptedException, ClassNotFoundException, NoSuchAlgorithmException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("adapt-mq.umiacs.umd.edu");
        factory.setVirtualHost(VHOST);
        Connection connection = factory.newConnection();
        connection.addShutdownListener(new ShutdownListener() {
            public void shutdownCompleted(ShutdownSignalException sse) {
                if ( sse.isHardError() ) {
                    LOG.fatal("Fatal Exception shutting down: " + sse.getReason());
                }else if (sse.isInitiatedByApplication()) {
                    LOG.error("Application Exception: " + sse.getReason());
                }

                LOG.info("Shutting down or something" + sse.getReason());
            }

        });
        Channel channel = connection.createChannel();

        // Make our exchange if necessary and bind our queue 
        channel.exchangeDeclare(EXCHANGE, "topic", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE, bindingKey);

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, consumer);


        while ( RUN ) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery(15000);
            // check for timeout
            if (delivery == null) {
                continue;
            }

            System.out.println("Got delivery");
            // Create an object from the byte stream
            ByteArrayInputStream bais = new ByteArrayInputStream(delivery.getBody());
            ObjectInputStream ois = new ObjectInputStream(bais);
            ChronBody body = (ChronBody)ois.readObject();

        }
        channel.close();
        connection.close();
    }

    public void setRun(boolean run) {
        this.RUN = run;
    }

    private int processJSON(JSONObject obj) throws NoSuchAlgorithmException {

        String depositor = (String) obj.get("depositor");
        String site = (String) obj.get("url");

        // Maybe full path instead
        String filename = (String) obj.get("filename");
        String digestType = (String) obj.get("digest-type");
        String digest = (String) obj.get("digest");

        System.out.println("Pulling file: " + filename);
        System.out.println("Digest to check against: " + digest);
        System.out.println("Digest method: " + digestType);
        System.out.println("Depositor: " + depositor);
        System.out.println("URL: " + site);


        MessageDigest md = null;// MessageDigest.getInstance(digestType);

        // Stop here while we don't have a server to pull from
        if (null != obj) {
            System.out.println("Returning 0");
            return 0;
        }

        // Break out to new class 
        try {
            // Make HTTP Connection
            URL url = new URL(site);
            ReadableByteChannel rbc = Channels.newChannel(url.openStream());

            FileOutputStream fos = new FileOutputStream(filename);
            FileChannel fc = fos.getChannel();
            ByteBuffer buf = ByteBuffer.allocate(blockSize);

            // Write file and update digest
            while ( rbc.read(buf) > 0 ) {
                // Do we want to throw an exception if write < 0?
                int write = fc.write(buf);
                md.update(buf);
                // buf.clear(); // I believe read takes care of this, will test later
            }
            fc.close();

            // Check digests
            // byte[] calculatedDigest = md.digest()
            // convert to String
            // compare
            // return 1 if false



        } catch (IOException ex) {
            LOG.fatal(ex);
        }

        // Made it here? Ack
        return 0;
    }

    @Override
    public void run() {
        try {
            consume();
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(AMQPFileConsumer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(AMQPFileConsumer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(AMQPFileConsumer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
            java.util.logging.Logger.getLogger(AMQPFileConsumer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
