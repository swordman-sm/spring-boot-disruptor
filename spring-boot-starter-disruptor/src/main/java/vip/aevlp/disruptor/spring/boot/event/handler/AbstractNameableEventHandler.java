package vip.aevlp.disruptor.spring.boot.event.handler;


import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;

public abstract class AbstractNameableEventHandler<T extends DisruptorEvent> implements DisruptorHandler<T>, Nameable {

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
