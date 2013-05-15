/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.file;

import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronHeader;
import java.io.IOException;
import java.util.Map;
import org.chronopolis.messaging.MessageType;

/**
 * Deprecated class, file transfers information will be done through
 * queries when required
 *
 * @author shake
 */
@Deprecated
public class FileTransferMessage extends ChronMessage2 {
	private static final String DEPOSIT_KEY = "depositor";
	private static final String DIGEST_KEY = "digest";
	private static final String DIGEST_TYPE_KEY = "digest-type";
	private static final String FILENAME_KEY = "filename";
	private static final String LOCATION_KEY = "location";
	
	public FileTransferMessage(MessageType type) {
        super(MessageType.DISTRIBUTE_INIT_ACK);
		this.body = new ChronBody(type);
		this.header = new ChronHeader();
	}

	public FileTransferMessage(MessageType type, ChronHeader header, ChronBody body) {
		// TODO: Parse all three
        super(MessageType.DISTRIBUTE_INIT_ACK);
        this.header = header;
        this.body = new ChronBody(type, body.getBody());
	}
	
    // Methods for setting header information
    
	public void setSource(String source) {
		header.setOrigin(source);
	}

	public void setCorrelationId(String correlationId) {
		header.setCorrelationId(correlationId);
	}
	
	public void setReturnKey(String returnKey) {
		header.setReturnKey(returnKey);
	}

	private void setTimestamp(String timestamp) {
		header.setDate(timestamp);
	}
	
    // Methods for the message body
    
	public void setDepositor(String depositor) {
		body.addContent(DEPOSIT_KEY, depositor);
	}
	
    /*
     * Digest to check against when pulling
     * 
     */
	public void setDigest(String digest) {
		body.addContent(DIGEST_KEY, digest);
	}
	
    /*
     * Type of digest for the file, should always be SHA-256
     * 
     */
	public void setDigestType(String digestType) {
		body.addContent(DIGEST_TYPE_KEY, digestType);
	}
	
    /*
     * The full path name so that we can create the file + parent dirs if needed
     * 
     */
	public void setFilename(String filename) {
		body.addContent(FILENAME_KEY, filename);
	}

    /*
     * The location of the file, most likely a url to grab it. Can also be an
     * rsync command if we decide to go with that
     * 
     */
    public void setLocation(String location) {
        body.addContent(LOCATION_KEY, location);
    }
	
	/**
	 * Creates the correlationID and timestamp for the header, 
	 * and uses the body for the input stream
	 * @return byte array consisting of the message body
	 * @throws IOException
	@Override
	public byte[] createMessage() throws IOException {
		setTimestamp(String.valueOf(System.currentTimeMillis()));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(body);
		return baos.toByteArray();
	}
	 */

    @Override
    public void processMessage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public Map<String, Object> getHeaderMap() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
	
}
