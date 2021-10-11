package vip.aevlp.disruptor.spring.boot.hooks;

import com.lmax.disruptor.dsl.Disruptor;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;

public class DisruptorShutdownHook extends Thread {

    private Disruptor<DisruptorEventT> disruptor;

    public DisruptorShutdownHook(Disruptor<DisruptorEventT> disruptor) {
        this.disruptor = disruptor;
    }

    @Override
    public void run() {
        disruptor.shutdown();
    }

}
