/*
 * zentity
 * Copyright © 2018-2022 Dave Moore
 * https://zentity.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.opensearch.plugin.zentity;

import org.opensearch.client.node.NodeClient;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;

import java.util.List;
import java.util.Properties;

import static org.opensearch.rest.RestRequest.Method.GET;


public class HomeAction extends BaseRestHandler {

    @Override
    public List<Route> routes() {
        return List.of(
                new Route(GET, "_zentity")
        );
    }

    @Override
    public String getName() {
        return "zentity_plugin_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) {

        Properties props = ZentityPlugin.properties();
        Boolean pretty = restRequest.paramAsBoolean("pretty", false);

        return channel -> {
            XContentBuilder content = XContentFactory.jsonBuilder();
            if (pretty)
                content.prettyPrint();
            content.startObject();
            content.field("name", props.getProperty("name"));
            content.field("description", props.getProperty("description"));
            content.field("website", props.getProperty("zentity.website"));
            content.startObject("version");
            content.field("zentity", props.getProperty("zentity.version"));
            content.field("opensearch", props.getProperty("opensearch.version"));
            content.endObject();
            content.endObject();
            channel.sendResponse(new BytesRestResponse(RestStatus.OK, content));
        };
    }
}
