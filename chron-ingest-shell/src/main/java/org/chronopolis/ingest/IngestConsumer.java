/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.bagit.BagValidator;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 *
 * @author shake
 */
public class IngestConsumer {

    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }

    public static void main(String [] args) {
        GenericXmlApplicationContext context = new GenericXmlApplicationContext(
                "classpath:/rabbit-context.xml");
        boolean done = false;
        ChronProducer p = (ChronProducer) context.getBean("producer");
        while (!done) {
            
            BagValidator validator = new BagValidator(Paths.get("/scratch1/staging/acadis_database_02-02-2013"));
            Future<Boolean> f = validator.getFuture();
            try {
                Boolean o = f.get();
                if ( o ) {
                    /*
                    Map<Path, String> digests = validator.getValidDigests();
                    for ( Map.Entry<Path, String> entry : digests.entrySet()) {
                        System.out.println(entry.getKey() + " :: " + entry.getValue());
                    }
                    */
                    Path manifest = validator.getManifest(Paths.get("/scratch1/tokens/"));
                    System.out.println(manifest.toString());
                    System.out.println("Downloading manifest");
                    StringBuilder url = new StringBuilder("http://localhost/staging/tokens/");
                    url.append(manifest.getFileName());
                    HttpsTransfer xfer = new HttpsTransfer();
                    xfer.getFile(url.toString());
                }

            } catch (InterruptedException ex) {
                Logger.getLogger(IngestConsumer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ExecutionException ex) {
                Logger.getLogger(IngestConsumer.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(IngestConsumer.class.getName()).log(Level.SEVERE, null, ex);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }

            System.out.println("Enter 'q' to exit: ");
            if ("q".equalsIgnoreCase(readLine())) {
                System.out.println("Shutdting dinow");
                done = true;
            }
        }

        context.close();
        System.out.println("Closed for business");
    }
}
