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
package org.apache.tika.server.core;

import java.util.List;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.tika.config.ServiceLoader;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;

public class CompositeParseContextConfig implements ParseContextConfig {


    final List<ParseContextConfig> configs;

    public CompositeParseContextConfig() {
        configs = new ServiceLoader(CompositeParseContextConfig.class.getClassLoader())
                .loadServiceProviders(ParseContextConfig.class);
    }

    @Override
    public void configure(MultivaluedMap<String, String> httpHeaders, Metadata metadata,
                          ParseContext context) {
        for (ParseContextConfig config : configs) {
            config.configure(httpHeaders, metadata, context);
        }
    }
}
