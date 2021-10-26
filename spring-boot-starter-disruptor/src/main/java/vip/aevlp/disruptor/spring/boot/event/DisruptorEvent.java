package vip.aevlp.disruptor.spring.boot.event;

import java.util.EventObject;

/**
 * Disruptor消息封装实体
 */
public abstract class DisruptorEvent<T> extends EventObject {

    /**
     * 时间戳
     */
    private final long timestamp;
    /**
     * 消息主题
     */
    private String topic;
    /**
     * 消息标签
     */
    private String tag;
    /**
     * 消息key
     */
    private String key;
    /**
     * 消息体
     */
    private T body;

    public DisruptorEvent(Object source) {
        super(source);
        this.timestamp = System.currentTimeMillis();
    }

    public String getRouteExpression() {
        return "/" + getTopic() + "/" + getTag() + "/" +
                getKey();

    }

    public void setSource(Object object) {
        this.source = source;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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
                ", topic='" + topic + '\'' +
                ", tag='" + tag + '\'' +
                ", key='" + key + '\'' +
                ", body=" + body +
                '}';
    }
}
