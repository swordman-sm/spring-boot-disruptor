/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package vip.aevlp.disruptor.spring.boot.handlers;

import org.springframework.stereotype.Component;
import vip.aevlp.disruptor.spring.boot.annotation.DisruptorMapping;
import vip.aevlp.disruptor.spring.boot.event.DisruptorBindEvent;
import vip.aevlp.disruptor.spring.boot.event.handler.DisruptorHandler;
import vip.aevlp.disruptor.spring.boot.event.handler.chain.HandlerChain;


@DisruptorMapping(router = "/Event-Output/TagA-Output/**")
@Component("emailHandler")
public class EmailDisruptorHandler implements DisruptorHandler<DisruptorBindEvent> {

    @Override
    public void doHandler(DisruptorBindEvent event, HandlerChain<DisruptorBindEvent> handlerChain) throws Exception {
        System.out.println("emailHandler: " + event.toString());
    }

}
