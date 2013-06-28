/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.chronopolis.messaging.MessageConstant.CORRELATION_ID;
import static org.chronopolis.messaging.MessageConstant.ORIGIN;
import static org.chronopolis.messaging.MessageConstant.DATE;
import static org.chronopolis.messaging.MessageConstant.RETURN_KEY;

/**
 *
 * @author shake
 */
public class ChronHeader {
    private Map<String, Object> header = new ConcurrentHashMap<>();
    private String origin;
    private String returnKey;
    private String correlationId;
    private String date;

    public ChronHeader() {
    }

	public ChronHeader(Map<String, Object> header) {
		// TODO: Parse header to make sure we have all the requirements
        origin = (String) header.get(ORIGIN.toString());
        returnKey = (String) header.get(RETURN_KEY.toString());
        correlationId = (String) header.get(CORRELATION_ID.toString());
        date = (String) header.get(DATE.toString());
        putAll();
	}

    private void putAll() {
        if ( header == null ) { 
            header = new ConcurrentHashMap<>();
        }
        header.put(ORIGIN.toString(), origin);
        header.put(RETURN_KEY.toString(), returnKey);
        header.put(CORRELATION_ID.toString(), correlationId);
        header.put(DATE.toString(), date);
    } 

    public void setOrigin(String origin) {
        this.origin = origin;
        header.put(ORIGIN.toString(), origin);
    }

    public void setReturnKey(String returnKey) {
        this.returnKey = returnKey;
        header.put(RETURN_KEY.toString(), returnKey);
    } 

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        header.put(CORRELATION_ID.toString(), correlationId);
    }

    public void setDate(String date) {
        this.date = date;
        header.put(DATE.toString(), date);
    }

	public Map<String, Object> getHeader() {
		return header;
	}

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("origin : ");
        sb.append(origin);
        sb.append(", return-key : ");
        sb.append(returnKey);
        sb.append(", date : ");
        sb.append(date);
        sb.append(", correlation-id");
        sb.append(correlationId);
        return sb.toString();
    }

    
}
