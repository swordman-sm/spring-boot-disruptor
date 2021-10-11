package vip.aevlp.disruptor.spring.boot.event.handler.chain;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;

/**
 * @author Steve
 */
public interface HandlerChain<T extends DisruptorEventT> {

    /**
     * @param event
     * @throws Exception
     */
    void doHandler(T event) throws Exception;

}
