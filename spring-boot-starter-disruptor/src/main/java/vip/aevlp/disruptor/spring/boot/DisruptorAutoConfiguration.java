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
     * 处理器链定义
     */
    private Map<String, String> handlerChainDefinitionMap = new HashMap<String, String>();

    /**
     * 决定一个消费者将如何等待生产者将Event置入Disruptor的策略。用来权衡当生产者无法将新的事件放进RingBuffer时的处理策略。
     * （例如：当生产者太快，消费者太慢，会导致生成者获取不到新的事件槽来插入新事件，则会根据该策略进行处理，默认会堵塞）
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
     * Handler实现集合
     */
    @Bean("disruptorHandlers")
    public Map<String, DisruptorHandler<DisruptorEvent>> disruptorHandlers() {

        Map<String, DisruptorHandler<DisruptorEvent>> disruptorPreHandlers = new LinkedHashMap<>();

        Map<String, DisruptorHandler> beansOfType = getApplicationContext().getBeansOfType(DisruptorHandler.class);
        if (!ObjectUtils.isEmpty(beansOfType)) {
            for (Entry<String, DisruptorHandler> entry : beansOfType.entrySet()) {
                if (entry.getValue() instanceof DisruptorEventDispatcher) {
                    // 跳过入口实现类
                    continue;
                }

                DisruptorMapping annotationType = getApplicationContext().findAnnotationOnBean(entry.getKey(), DisruptorMapping.class);
                if (annotationType == null) {
                    // 注解为空，则打印错误信息
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
     * 处理器链集合
     */
    @Bean("disruptorEventHandlers")
    public List<DisruptorEventDispatcher> disruptorEventHandlers(DisruptorProperties properties,
                                                                 @Qualifier("disruptorHandlers") Map<String, DisruptorHandler<DisruptorEvent>> eventHandlers) {
        // 获取定义 拦截链规则
        List<RouterHandler> handlerDefinitions = properties.getHandlerDefinitions();
        // 拦截器集合
        List<DisruptorEventDispatcher> disruptorEventHandlers = new ArrayList<DisruptorEventDispatcher>();
        // 未定义，则使用默认规则
        if (CollectionUtils.isEmpty(handlerDefinitions)) {

            RouterHandler definition = new RouterHandler();

            definition.setOrder(0);
            definition.setDefinitionMap(handlerChainDefinitionMap);

            // 构造DisruptorEventHandler
            disruptorEventHandlers.add(this.createDisruptorEventHandler(definition, eventHandlers));

        } else {
            // 迭代拦截器规则
            for (RouterHandler handlerDefinition : handlerDefinitions) {

                // 构造DisruptorEventHandler
                disruptorEventHandlers.add(this.createDisruptorEventHandler(handlerDefinition, eventHandlers));

            }
        }
        // 进行排序
        Collections.sort(disruptorEventHandlers, new OrderComparator());

        return disruptorEventHandlers;
    }

    /*
     * 构造DisruptorEventHandler
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
                // ant匹配规则
                String rule = entry.getKey();
                String chainDefinition = entry.getValue();
                manager.createChain(rule, chainDefinition);
            }
        }

        return manager;
    }

    /**
     * <p>
     * 创建Disruptor
     * </p>
     * <p>
     * 1 eventFactory 为
     * <p>
     * 2 ringBufferSize为RingBuffer缓冲区大小，最好是2的指数倍
     * </p>
     *
     * @param properties             : 配置参数
     * @param waitStrategy           : 一种策略，用来均衡数据生产者和消费者之间的处理效率，默认提供了3个实现类
     * @param threadFactory          : 线程工厂
     * @param eventFactory           : 工厂类对象，用于创建一个个的LongEvent， LongEvent是实际的消费数据，初始化启动Disruptor的时候，Disruptor会调用该工厂方法创建一个个的消费数据实例存放到RingBuffer缓冲区里面去，创建的对象个数为ringBufferSize指定的
     * @param disruptorEventHandlers : 事件分发器
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

            // 进行排序
            Collections.sort(disruptorEventHandlers, new OrderComparator());

            // 使用disruptor创建消费者组
            EventHandlerGroup<DisruptorEvent> handlerGroup = null;
            for (int i = 0; i < disruptorEventHandlers.size(); i++) {
                // 连接消费事件方法，其中EventHandler的是为消费者消费消息的实现类
                DisruptorEventDispatcher eventHandler = disruptorEventHandlers.get(i);
                if (i < 1) {
                    handlerGroup = disruptor.handleEventsWith(eventHandler);
                } else {
                    // 完成前置事件处理之后执行后置事件处理
                    handlerGroup.then(eventHandler);
                }
            }
        }

        // 启动
        disruptor.start();

        /**
         * 应用退出时，要调用shutdown来清理资源，关闭网络连接，从MetaQ服务器上注销自己
         * 注意：我们建议应用在JBOSS、Tomcat等容器的退出钩子里调用shutdown方法
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
