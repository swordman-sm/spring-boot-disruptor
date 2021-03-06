package vip.aevlp.disruptor.spring.boot.hooks;

import com.lmax.disruptor.dsl.Disruptor;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;

public class DisruptorShutdownHook extends Thread {

    private Disruptor<DisruptorEvent> disruptor;

    public DisruptorShutdownHook(Disruptor<DisruptorEvent> disruptor) {
        this.disruptor = disruptor;
    }

    @Override
    public void run() {
        disruptor.shutdown();
    }

}
