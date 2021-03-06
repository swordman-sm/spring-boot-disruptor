package vip.aevlp.disruptor.spring.boot;

import com.lmax.disruptor.*;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.OrderComparator;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import vip.aevlp.disruptor.spring.boot.annotation.DisruptorMapping;
import vip.aevlp.disruptor.spring.boot.config.RouterHandler;
import vip.aevlp.disruptor.spring.boot.config.Init;
import vip.aevlp.disruptor.spring.boot.config.Section;
import vip.aevlp.disruptor.spring.boot.context.DisruptorEventAwareProcessor;
import vip.aevlp.disruptor.spring.boot.event.DisruptorApplicationEvent;
import vip.aevlp.disruptor.spring.boot.event.DisruptorEvent;
import vip.aevlp.disruptor.spring.boot.event.factory.DisruptorBindEventFactory;
import vip.aevlp.disruptor.spring.boot.event.factory.DisruptorEventThreadFactory;
import vip.aevlp.disruptor.spring.boot.event.handler.DisruptorEventDispatcher;
import vip.aevlp.disruptor.spring.boot.event.handler.DisruptorHandler;
import vip.aevlp.disruptor.spring.boot.event.handler.Nameable;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChainManager;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.def.DefaultHandlerChainManager;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.def.PathMatchingHandlerChainResolver;
import vip.aevlp.disruptor.spring.boot.event.translator.DisruptorEventOneArgTranslator;
import vip.aevlp.disruptor.spring.boot.event.translator.DisruptorEventThreeArgTranslator;
import vip.aevlp.disruptor.spring.boot.event.translator.DisruptorEventTwoArgTranslator;
import vip.aevlp.disruptor.spring.boot.hooks.DisruptorShutdownHook;
import vip.aevlp.disruptor.spring.boot.util.StringUtils;
import vip.aevlp.disruptor.spring.boot.util.WaitStrategys;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ThreadFactory;

@Configuration
@ConditionalOnClass({Disruptor.class})
@ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "enabled", havingValue = "true")
@EnableConfigurationProperties({DisruptorProperties.class})
@SuppressWarnings({"unchecked", "rawtypes"})
public class DisruptorAutoConfiguration implements ApplicationContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(DisruptorAutoConfiguration.class);
    private ApplicationContext applicationContext;
    /**
     * ??????????????????
     */
    private Map<String, String> handlerChainDefinitionMap = new HashMap<String, String>();

    /**
     * ????????????????????????????????????????????????Event??????Disruptor???????????????????????????????????????????????????????????????RingBuffer?????????????????????
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     *
     * @return {@link WaitStrategy} instance
     */
    @Bean
    @ConditionalOnMissingBean
    public WaitStrategy waitStrategy() {
        return WaitStrategys.YIELDING_WAIT;
    }

    @Bean
    @ConditionalOnMissingBean
    public ThreadFactory threadFactory() {
        return new DisruptorEventThreadFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventFactory<DisruptorEvent> eventFactory() {
        return new DisruptorBindEventFactory();
    }

    /*
     * Handler????????????
     */
    @Bean("disruptorHandlers")
    public Map<String, DisruptorHandler<DisruptorEvent>> disruptorHandlers() {

        Map<String, DisruptorHandler<DisruptorEvent>> disruptorPreHandlers = new LinkedHashMap<>();

        Map<String, DisruptorHandler> beansOfType = getApplicationContext().getBeansOfType(DisruptorHandler.class);
        if (!ObjectUtils.isEmpty(beansOfType)) {
            for (Entry<String, DisruptorHandler> entry : beansOfType.entrySet()) {
                if (entry.getValue() instanceof DisruptorEventDispatcher) {
                    // ?????????????????????
                    continue;
                }

                DisruptorMapping annotationType = getApplicationContext().findAnnotationOnBean(entry.getKey(), DisruptorMapping.class);
                if (annotationType == null) {
                    // ????????????????????????????????????
                    LOG.error("Not Found AnnotationType {} on Bean {} Whith Name {}", DisruptorMapping.class, entry.getValue().getClass(), entry.getKey());
                } else {
                    handlerChainDefinitionMap.put(annotationType.router(), entry.getKey());
                }

                disruptorPreHandlers.put(entry.getKey(), entry.getValue());
            }
        }
        // BeanFactoryUtils.beansOfTypeIncludingAncestors(getApplicationContext(),
        // EventHandler.class);

        return disruptorPreHandlers;
    }

    /*
     * ??????????????????
     */
    @Bean("disruptorEventHandlers")
    public List<DisruptorEventDispatcher> disruptorEventHandlers(DisruptorProperties properties,
                                                                 @Qualifier("disruptorHandlers") Map<String, DisruptorHandler<DisruptorEvent>> eventHandlers) {
        // ???????????? ???????????????
        List<RouterHandler> handlerDefinitions = properties.getHandlerDefinitions();
        // ???????????????
        List<DisruptorEventDispatcher> disruptorEventHandlers = new ArrayList<DisruptorEventDispatcher>();
        // ?????????????????????????????????
        if (CollectionUtils.isEmpty(handlerDefinitions)) {

            RouterHandler routerHandler = new RouterHandler();

            routerHandler.setOrder(0);
            routerHandler.setDefinitionMap(handlerChainDefinitionMap);

            // ??????DisruptorEventHandler
            disruptorEventHandlers.add(this.createDisruptorEventHandler(routerHandler, eventHandlers));

        } else {
            // ?????????????????????
            for (RouterHandler handlerDefinition : handlerDefinitions) {

                // ??????DisruptorEventHandler
                disruptorEventHandlers.add(this.createDisruptorEventHandler(handlerDefinition, eventHandlers));

            }
        }
        // ????????????
        disruptorEventHandlers.sort(new OrderComparator());

        return disruptorEventHandlers;
    }

    /*
     * ??????DisruptorEventHandler
     */
    private DisruptorEventDispatcher createDisruptorEventHandler(RouterHandler handlerDefinition,
                                                                 Map<String, DisruptorHandler<DisruptorEvent>> eventHandlers) {

        if (StringUtils.isNotEmpty(handlerDefinition.getRouters())) {
            handlerChainDefinitionMap.putAll(this.parseHandlerChainDefinitions(handlerDefinition.getRouters()));
        } else if (!CollectionUtils.isEmpty(handlerDefinition.getDefinitionMap())) {
            handlerChainDefinitionMap.putAll(handlerDefinition.getDefinitionMap());
        }

        HandlerChainManager<DisruptorEvent> manager = createHandlerChainManager(eventHandlers, handlerChainDefinitionMap);
        PathMatchingHandlerChainResolver chainResolver = new PathMatchingHandlerChainResolver();
        chainResolver.setHandlerChainManager(manager);
        return new DisruptorEventDispatcher(chainResolver, handlerDefinition.getOrder());
    }

    private Map<String, String> parseHandlerChainDefinitions(String definitions) {
        Init init = new Init();
        init.load(definitions);
        Section section = init.getSection("urls");
        if (CollectionUtils.isEmpty(section)) {
            section = init.getSection(Init.DEFAULT_SECTION_NAME);
        }
        return section;
    }

    private HandlerChainManager<DisruptorEvent> createHandlerChainManager(
            Map<String, DisruptorHandler<DisruptorEvent>> eventHandlers,
            Map<String, String> handlerChainDefinitionMap) {

        HandlerChainManager<DisruptorEvent> manager = new DefaultHandlerChainManager();
        if (!CollectionUtils.isEmpty(eventHandlers)) {
            for (Entry<String, DisruptorHandler<DisruptorEvent>> entry : eventHandlers.entrySet()) {
                String name = entry.getKey();
                DisruptorHandler<DisruptorEvent> handler = entry.getValue();
                if (handler instanceof Nameable) {
                    ((Nameable) handler).setName(name);
                }
                manager.addHandler(name, handler);
            }
        }

        if (!CollectionUtils.isEmpty(handlerChainDefinitionMap)) {
            for (Entry<String, String> entry : handlerChainDefinitionMap.entrySet()) {
                // ant????????????
                String rule = entry.getKey();
                String chainDefinition = entry.getValue();
                manager.createChain(rule, chainDefinition);
            }
        }

        return manager;
    }

    /**
     * <p>
     * ??????Disruptor
     * </p>
     * <p>
     * 1 eventFactory ???
     * <p>
     * 2 ringBufferSize???RingBuffer???????????????????????????2????????????
     * </p>
     *
     * @param properties             : ????????????
     * @param waitStrategy           : ?????????????????????????????????????????????????????????????????????????????????????????????3????????????
     * @param threadFactory          : ????????????
     * @param eventFactory           : ??????????????????????????????????????????LongEvent??? LongEvent??????????????????????????????????????????Disruptor????????????Disruptor?????????????????????????????????????????????????????????????????????RingBuffer?????????????????????????????????????????????ringBufferSize?????????
     * @param disruptorEventHandlers : ???????????????
     * @return {@link Disruptor} instance
     */
    @Bean
    @ConditionalOnClass({Disruptor.class})
    @ConditionalOnProperty(prefix = DisruptorProperties.PREFIX, value = "enabled", havingValue = "true")
    public Disruptor<DisruptorEvent> disruptor(
            DisruptorProperties properties,
            WaitStrategy waitStrategy,
            ThreadFactory threadFactory,
            EventFactory<DisruptorEvent> eventFactory,
            @Qualifier("disruptorEventHandlers")
                    List<DisruptorEventDispatcher> disruptorEventHandlers) {

        // http://blog.csdn.net/a314368439/article/details/72642653?utm_source=itdadao&utm_medium=referral

        Disruptor<DisruptorEvent> disruptor = null;
        if (properties.isMultiProducer()) {
            disruptor = new Disruptor<>(eventFactory, properties.getRingBufferSize(), threadFactory,
                    ProducerType.MULTI, waitStrategy);
        } else {
            disruptor = new Disruptor<>(eventFactory, properties.getRingBufferSize(), threadFactory,
                    ProducerType.SINGLE, waitStrategy);
        }

        if (!ObjectUtils.isEmpty(disruptorEventHandlers)) {

            // ????????????
            Collections.sort(disruptorEventHandlers, new OrderComparator());

            // ??????disruptor??????????????????
            EventHandlerGroup<DisruptorEvent> handlerGroup = null;
            for (int i = 0; i < disruptorEventHandlers.size(); i++) {
                // ?????????????????????????????????EventHandler??????????????????????????????????????????
                DisruptorEventDispatcher eventHandler = disruptorEventHandlers.get(i);
                if (i < 1) {
                    handlerGroup = disruptor.handleEventsWith(eventHandler);
                } else {
                    // ??????????????????????????????????????????????????????
                    handlerGroup.then(eventHandler);
                }
            }
        }

        // ??????
        disruptor.start();

        /**
         * ???????????????????????????shutdown??????????????????????????????????????????MetaQ????????????????????????
         * ??????????????????????????????JBOSS???Tomcat?????????????????????????????????shutdown??????
         */
        Runtime.getRuntime().addShutdownHook(new DisruptorShutdownHook(disruptor));

        return disruptor;

    }

    @Bean
    @ConditionalOnMissingBean
    public EventTranslatorOneArg<DisruptorEvent, DisruptorEvent> oneArgEventTranslator() {
        return new DisruptorEventOneArgTranslator();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventTranslatorTwoArg<DisruptorEvent, String, String> twoArgEventTranslator() {
        return new DisruptorEventTwoArgTranslator();
    }

    @Bean
    @ConditionalOnMissingBean
    public EventTranslatorThreeArg<DisruptorEvent, String, String, String> threeArgEventTranslator() {
        return new DisruptorEventThreeArgTranslator();
    }

    @Bean
    public DisruptorTemplate disruptorTemplate() {
        return new DisruptorTemplate();
    }

    @Bean
    public ApplicationListener<DisruptorApplicationEvent> disruptorEventListener(Disruptor<DisruptorEvent> disruptor,
                                                                                 EventTranslatorOneArg<DisruptorEvent, DisruptorEvent> oneArgEventTranslator) {
        return appEvent -> {
            DisruptorEvent event = (DisruptorEvent) appEvent.getSource();
            disruptor.publishEvent(oneArgEventTranslator, event);
        };
    }

    @Bean
    public DisruptorEventAwareProcessor disruptorEventAwareProcessor() {
        return new DisruptorEventAwareProcessor();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private ApplicationContext getApplicationContext() {
        return applicationContext;
    }

}
