package comp1206.sushi;

import java.io.Serializable;
import java.time.LocalTime;

/**
 * Wrapper class for the messages sent between the clients and the server
 * Each message has a String that describes its' contents
 */

public class MessageWrapper implements Serializable {

    private String msgStatus;
    private Object msgContent;
    private LocalTime creationTime;

    /**
     * Constructor for the MessageWrapper class
     */

    public MessageWrapper(String msgStatus, Object msgContent) {
        this.msgContent = msgContent;
        this.msgStatus = msgStatus;
        this.creationTime = LocalTime.now();
    }

    /**
     * Getter for the message status string
     * @return msgStatus
     */

    public String getMsgStatus() {return msgStatus; }

    /**
     * Getter for the message content object
     * @return msgContent
     */

    public Object getMsgContent() { return msgContent;}


    /**
     * Getter for the creation time of the message
     * @return creationTime
     */

    public LocalTime getCreationTime() {
        return creationTime;
    }
}
