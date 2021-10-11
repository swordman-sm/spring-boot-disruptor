package vip.aevlp.disruptor.spring.boot.event;

import java.util.EventObject;

/**
 * Disruptor消息封装实体
 */
@SuppressWarnings("serial")
public abstract class DisruptorEventT<T> extends EventObject {

    /**
     * System time when the event happened
     */
    private final long timestamp;
    /**
     * Event Name
     */
    private String event;
    /**
     * Event Tag
     */
    private String tag;
    /**
     * Event Keys
     */
    private String key;
    /**
     * Event body
     */
    private T body;

    /**
     * Create a new ConsumeEvent.
     *
     * @param source the object on which the event initially occurred (never {@code null})
     */
    public DisruptorEventT(Object source) {
        super(source);
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Return the system time in milliseconds when the event happened.
     *
     * @return system time in milliseconds
     */
    public final long getTimestamp() {
        return this.timestamp;
    }

    public String getRouteExpression() {
        return "/" + getEvent() + "/" + getTag() + "/" +
                getKey();

    }

    public void setSource(Object source) {
        this.source = source;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public T getBody() {
        return body;
    }

    public void setBody(T body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "DisruptorEvent{" +
                "timestamp=" + timestamp +
                ", event='" + event + '\'' +
                ", tag='" + tag + '\'' +
                ", key='" + key + '\'' +
                ", body=" + body +
                '}';
    }
}
