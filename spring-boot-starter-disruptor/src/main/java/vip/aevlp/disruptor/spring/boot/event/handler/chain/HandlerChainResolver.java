package vip.aevlp.disruptor.spring.boot.event.handler.chain;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;

public interface HandlerChainResolver<T extends DisruptorEventT> {

	HandlerChain<T> getChain(T event, HandlerChain<T> originalChain);
	
}
