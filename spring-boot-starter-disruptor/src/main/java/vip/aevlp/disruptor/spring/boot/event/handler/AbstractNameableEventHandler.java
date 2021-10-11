package vip.aevlp.disruptor.spring.boot.event.handler;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;

public abstract class AbstractNameableEventHandler<T extends DisruptorEventT> implements DisruptorHandler<T>, Nameable {

	/**
	 * 过滤器名称
	 */
	protected String name;

	String getName() {
		return this.name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

}
