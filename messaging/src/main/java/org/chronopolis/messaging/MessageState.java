/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging;

/**
 * All of our processes are begun by some specific node--that node is the "originator" of the process and
 * thus all of the messages it sends will be begin with 'o'.  All messages the other nodes send to it will
 * be begin with 'r'.  This is not strictly necessary to establish the desired 1:1 symbol:semantic mapping,
 * but I found it to help with organization--you know your listener should never be sending a message marked
 * with 'o-'. 
 * @author toaster
 */
public enum MessageState {

    ORIGIN("o"),
    RESPONSE("r");

    private MessageState(String name) {
        this.name = name;
    }
    private String name;

    public String getName() {
        return name;
    }

    public MessageState valueOf(char c) {

        switch (Character.toLowerCase(c)) {
            case 'o':
                return ORIGIN;
            case 'r':
                return RESPONSE;
            default:
                throw new IllegalArgumentException();

        }
    }

    public MessageState find(String s) {
        if (null == s) {
            throw new NullPointerException();
        }

        switch (s.toLowerCase()) {
            case "o":
                return ORIGIN;
            case "r":
                return RESPONSE;
            default:
                throw new IllegalArgumentException();

        }

    }
}
