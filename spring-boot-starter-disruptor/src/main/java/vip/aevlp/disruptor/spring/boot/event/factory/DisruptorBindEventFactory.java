package vip.aevlp.disruptor.spring.boot.event.factory;

import com.lmax.disruptor.EventFactory;
import vip.aevlp.disruptor.spring.boot.event.DisruptorBindEvent;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;

/**
 * @author Steve
 */
public class DisruptorBindEventFactory implements EventFactory<DisruptorEvent> {

    @Override
    public DisruptorEvent newInstance() {
        return new DisruptorBindEvent(this);
    }

}
