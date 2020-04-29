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
package com.alipay.sofa.ark.container.service.biz;

import com.alipay.sofa.ark.common.util.AssertUtils;
import com.alipay.sofa.ark.container.model.BizModel;
import com.alipay.sofa.ark.container.model.PluginContextImpl;
import com.alipay.sofa.ark.container.model.PluginModel;
import com.alipay.sofa.ark.container.service.classloader.BizClassLoader;
import com.alipay.sofa.ark.loader.JarBizArchive;
import com.alipay.sofa.ark.loader.JarPluginArchive;
import com.alipay.sofa.ark.loader.archive.JarFileArchive;
import com.alipay.sofa.ark.loader.jar.JarFile;
import com.alipay.sofa.ark.spi.archive.Archive;
import com.alipay.sofa.ark.spi.archive.BizArchive;
import com.alipay.sofa.ark.spi.constant.Constants;
import com.alipay.sofa.ark.spi.model.Biz;
import com.alipay.sofa.ark.spi.model.BizState;
import com.alipay.sofa.ark.spi.model.Plugin;
import com.alipay.sofa.ark.spi.service.biz.BizFactoryService;
import com.alipay.sofa.ark.spi.service.plugin.PluginManagerService;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Attributes;

import static com.alipay.sofa.ark.spi.constant.Constants.ACTIVATOR_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.ARK_BIZ_NAME;
import static com.alipay.sofa.ark.spi.constant.Constants.ARK_BIZ_VERSION;
import static com.alipay.sofa.ark.spi.constant.Constants.ARTIFACT_ID_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.DENY_IMPORT_CLASSES;
import static com.alipay.sofa.ark.spi.constant.Constants.DENY_IMPORT_PACKAGES;
import static com.alipay.sofa.ark.spi.constant.Constants.DENY_IMPORT_RESOURCES;
import static com.alipay.sofa.ark.spi.constant.Constants.EXPORT_CLASSES_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.EXPORT_PACKAGES_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.EXPORT_RESOURCES_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.GROUP_ID_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.IMPORT_CLASSES_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.IMPORT_PACKAGES_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.IMPORT_RESOURCES_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.MAIN_CLASS_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.PLUGIN_NAME_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.PLUGIN_VERSION_ATTRIBUTE;
import static com.alipay.sofa.ark.spi.constant.Constants.PRIORITY_ATTRIBUTE;

/**
 * {@link BizFactoryService}
 *
 * @author qilong.zql
 * @since 0.4.0
 */
@Singleton
public class BizFactoryServiceImpl implements BizFactoryService {

    @Inject
    private PluginManagerService pluginManagerService;

    @Override
    public Biz createBiz(BizArchive bizArchive) throws IOException {
        AssertUtils.isTrue(isArkBiz(bizArchive), "Archive must be a ark biz!");

        Attributes manifestMainAttributes = bizArchive.getManifest().getMainAttributes();

        PluginModel bizPlugin = new PluginModel();
        JarPluginArchive jarPluginArchive = new JarPluginArchive(bizArchive);
        bizPlugin.setPluginName(manifestMainAttributes.getValue(PLUGIN_NAME_ATTRIBUTE))
                .setGroupId(manifestMainAttributes.getValue(GROUP_ID_ATTRIBUTE))
                .setArtifactId(manifestMainAttributes.getValue(ARTIFACT_ID_ATTRIBUTE))
                .setVersion(manifestMainAttributes.getValue(PLUGIN_VERSION_ATTRIBUTE))
                .setPriority(manifestMainAttributes.getValue(PRIORITY_ATTRIBUTE))
                .setPluginActivator(manifestMainAttributes.getValue(ACTIVATOR_ATTRIBUTE))
                .setClassPath(jarPluginArchive.getUrls()).setPluginUrl(jarPluginArchive.getUrl())
                .setExportClasses(manifestMainAttributes.getValue(EXPORT_CLASSES_ATTRIBUTE))
                .setExportPackages(manifestMainAttributes.getValue(EXPORT_PACKAGES_ATTRIBUTE))
                .setImportClasses(manifestMainAttributes.getValue(IMPORT_CLASSES_ATTRIBUTE))
                .setImportPackages(manifestMainAttributes.getValue(IMPORT_PACKAGES_ATTRIBUTE))
                .setExportIndex(jarPluginArchive.getExportIndex())
                .setImportResources(manifestMainAttributes.getValue(IMPORT_RESOURCES_ATTRIBUTE))
                .setExportResources(manifestMainAttributes.getValue(EXPORT_RESOURCES_ATTRIBUTE))
                .setPluginContext(new PluginContextImpl(bizPlugin));

        BizModel bizModel = new BizModel();
        bizModel.setBizState(BizState.RESOLVED)
                .setBizName(manifestMainAttributes.getValue(ARK_BIZ_NAME))
                .setBizVersion(manifestMainAttributes.getValue(ARK_BIZ_VERSION))
                .setMainClass(manifestMainAttributes.getValue(MAIN_CLASS_ATTRIBUTE))
                .setPriority(manifestMainAttributes.getValue(PRIORITY_ATTRIBUTE))
                .setDenyImportPackages(manifestMainAttributes.getValue(DENY_IMPORT_PACKAGES))
                .setDenyImportClasses(manifestMainAttributes.getValue(DENY_IMPORT_CLASSES))
                .setDenyImportResources(manifestMainAttributes.getValue(DENY_IMPORT_RESOURCES))
                .setClassPath(bizArchive.getUrls())
                .setBizPlugin(bizPlugin);

        ClassLoader classLoader = new BizClassLoader(bizModel.getIdentity(),
                getBizUcp(bizModel.getClassPath()));
        bizPlugin.setPluginClassLoader(classLoader);
        bizModel.setClassLoader(classLoader);

        return bizModel;
    }

    @Override
    public Biz createBiz(File file) throws IOException {
        JarFile bizFile = new JarFile(file);
        JarFileArchive jarFileArchive = new JarFileArchive(bizFile);
        JarBizArchive bizArchive = new JarBizArchive(jarFileArchive);
        return createBiz(bizArchive);
    }

    private boolean isArkBiz(BizArchive bizArchive) {
        return bizArchive.isEntryExist(new Archive.EntryFilter() {
            @Override
            public boolean matches(Archive.Entry entry) {
                return !entry.isDirectory() && entry.getName().equals(Constants.ARK_BIZ_MARK_ENTRY);
            }
        });
    }

    private URL[] getBizUcp(URL[] bizClassPath) {
        List<URL> bizUcp = new ArrayList<>();
        bizUcp.addAll(Arrays.asList(bizClassPath));
        bizUcp.addAll(Arrays.asList(getPluginURLs()));
        return bizUcp.toArray(new URL[bizUcp.size()]);
    }

    private URL[] getPluginURLs() {
        List<URL> pluginUrls = new ArrayList<>();
        for (Plugin plugin : pluginManagerService.getPluginsInOrder()) {
            pluginUrls.add(plugin.getPluginURL());
        }
        return pluginUrls.toArray(new URL[pluginUrls.size()]);
    }
}