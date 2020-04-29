/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alipay.sofa.ark.sample2.activator;

import com.alipay.sofa.ark.exception.ArkException;
import com.alipay.sofa.ark.sample.facade.SamplePluginService;
import com.alipay.sofa.ark.sample2.impl.SamplePluginServiceImpl;
import com.alipay.sofa.ark.spi.model.PluginContext;
import com.alipay.sofa.ark.spi.service.PluginActivator;

import java.util.concurrent.CompletableFuture;

/**
 * A sample ark-plugin activator
 *
 * @author qilong.zql
 * @since 0.1.0
 */
public class SamplePluginActivator implements PluginActivator {

    public void start(PluginContext context) throws ArkException {
        System.out.println("starting in sample ark plugin2 activator");
        context.publishService(SamplePluginService.class, new SamplePluginServiceImpl(),
                "SamplePluginService2");
        System.out.println(context
                .referenceService(SamplePluginService.class, "SamplePluginService2").getService()
                .service());
        CompletableFuture.runAsync(() -> {
            while (true) {
                try {
                    Object samplePluginService = context.referenceService(SamplePluginService.class, "SamplePluginService").getService();
                    System.out.println(samplePluginService.getClass());
                    System.out.println(samplePluginService.getClass().getMethod("service").invoke(samplePluginService));
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        });

    }

    public void stop(PluginContext context) throws ArkException {
        System.out.println("stopping in ark plugin activator");
    }

}