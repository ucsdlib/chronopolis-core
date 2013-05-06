/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.base;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        this.header.putAll(header);
	}

    public void setOrigin(String origin) {
        this.origin = origin;
        header.put("origin", origin);
    }

    public void setReturnKey(String returnKey) {
        this.returnKey = returnKey;
        header.put("returnKey", returnKey);
    } 

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        header.put("correlationId", correlationId);
    }

    public void setDate(String date) {
        this.date = date;
        header.put("date", date);
    }

	public Map<String, Object> getHeader() {
		return header;
	}

    
}
