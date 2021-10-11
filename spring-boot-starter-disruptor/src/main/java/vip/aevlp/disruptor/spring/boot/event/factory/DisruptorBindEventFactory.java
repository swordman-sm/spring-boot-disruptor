package vip.aevlp.disruptor.spring.boot.event.factory;

import com.lmax.disruptor.EventFactory;
import vip.aevlp.disruptor.spring.boot.event.DisruptorBindEvent;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEventT;

/**
 * @author Steve
 */
public class DisruptorBindEventFactory implements EventFactory<DisruptorEventT> {

    @Override
    public DisruptorEventT newInstance() {
        return new DisruptorBindEvent(this);
    }

}
