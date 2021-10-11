package vip.aevlp.disruptor.spring.boot.context.event;

/**
 * @author Steve
 */
public interface DisruptorEventProducerAware {

    /**
     * 设置生产者
     *
     * @param disruptorEventProducer
     */
    void setDisruptorEventPublisher(DisruptorEventProducer disruptorEventProducer);

}
