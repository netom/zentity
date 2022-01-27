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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.OpenSearchSecurityException;
import org.opensearch.action.ActionListener;
import org.opensearch.action.admin.indices.create.CreateIndexResponse;
import org.opensearch.client.node.NodeClient;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.xcontent.XContentBuilder;
import org.opensearch.common.xcontent.XContentFactory;
import org.opensearch.common.xcontent.XContentType;
import org.opensearch.rest.BaseRestHandler;
import org.opensearch.rest.BytesRestResponse;
import org.opensearch.rest.RestRequest;
import org.opensearch.rest.RestStatus;

import java.util.List;
import java.util.Properties;

import static org.opensearch.rest.RestRequest.Method;
import static org.opensearch.rest.RestRequest.Method.POST;

public class SetupAction extends BaseRestHandler {

    private static final Logger logger = LogManager.getLogger(SetupAction.class);

    public static final int DEFAULT_NUMBER_OF_SHARDS = 1;
    public static final int DEFAULT_NUMBER_OF_REPLICAS = 1;
    public static final String INDEX_MAPPING = "{\n" +
            "  \"dynamic\": \"strict\",\n" +
            "  \"properties\": {\n" +
            "    \"attributes\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"enabled\": false\n" +
            "    },\n" +
            "    \"resolvers\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"enabled\": false\n" +
            "    },\n" +
            "    \"matchers\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"enabled\": false\n" +
            "    },\n" +
            "    \"indices\": {\n" +
            "      \"type\": \"object\",\n" +
            "      \"enabled\": false\n" +
            "    }\n" +
            "  }\n" +
            "}";
    public static final String INDEX_MAPPING_ELASTICSEARCH_6 = "{\n" +
            "  \"doc\": " + INDEX_MAPPING + "\n" +
            "}";

    @Override
    public List<Route> routes() {
        return List.of(
                new Route(POST, "_zentity/_setup")
        );
    }

    /**
     * Create the .zentity-models index.
     *
     * @param client           The client that will communicate with OpenSearch.
     * @param numberOfShards   The value of index.number_of_shards.
     * @param numberOfReplicas The value of index.number_of_replicas.
     * @param onComplete       Action to perform after index creation request completes.
     * @return
     */
    public static void createIndex(NodeClient client, int numberOfShards, int numberOfReplicas, ActionListener<CreateIndexResponse> onComplete) {
        // OpenSearch 7.0.0+ removes mapping types
        Properties props = ZentityPlugin.properties();
        if (props.getProperty("opensearch.version").compareTo("7.") >= 0) {
            client.admin().indices().prepareCreate(ModelsAction.INDEX_NAME)
                .setSettings(Settings.builder()
                        .put("index.number_of_shards", numberOfShards)
                        .put("index.number_of_replicas", numberOfReplicas)
                )
                .addMapping("doc", INDEX_MAPPING, XContentType.JSON)
                .execute(onComplete);
        } else {
            client.admin().indices().prepareCreate(ModelsAction.INDEX_NAME)
                .setSettings(Settings.builder()
                        .put("index.number_of_shards", numberOfShards)
                        .put("index.number_of_replicas", numberOfReplicas)
                )
                .addMapping("doc", INDEX_MAPPING_ELASTICSEARCH_6, XContentType.JSON)
                .execute(onComplete);
        }
    }

    /**
     * Create the .zentity-models index using the default settings.
     *
     * @param client     The client that will communicate with OpenSearch.
     * @param onComplete The action to perform after the index creation request completes.
     */
    public static void createIndex(NodeClient client, ActionListener<CreateIndexResponse> onComplete) {
        createIndex(client, DEFAULT_NUMBER_OF_SHARDS, DEFAULT_NUMBER_OF_REPLICAS, onComplete);
    }

    @Override
    public String getName() {
        return "zentity_setup_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest restRequest, NodeClient client) {

        // Parse request
        Boolean pretty = restRequest.paramAsBoolean("pretty", false);
        int numberOfShards = restRequest.paramAsInt("number_of_shards", DEFAULT_NUMBER_OF_SHARDS);
        int numberOfReplicas = restRequest.paramAsInt("number_of_replicas", DEFAULT_NUMBER_OF_REPLICAS);
        Method method = restRequest.method();

        return channel -> {
            try {
                if (method == POST) {
                    createIndex(client, numberOfShards, numberOfReplicas, new ActionListener<>() {

                        @Override
                        public void onResponse(CreateIndexResponse response) {
                            try {

                                // The .zentity-models index was created. Send the response.
                                XContentBuilder content = XContentFactory.jsonBuilder();
                                if (pretty)
                                    content.prettyPrint();
                                content.startObject().field("acknowledged", true).endObject();
                                channel.sendResponse(new BytesRestResponse(RestStatus.OK, content));
                            } catch (Exception e) {

                                // An error occurred when sending the response.
                                ZentityPlugin.sendResponseError(channel, logger, e);
                            }
                        }

                        @Override
                        public void onFailure(Exception e) {

                            // An error occurred when creating the .zentity-models index.
                            if (e.getClass() == OpenSearchSecurityException.class) {

                                // The error was a security exception.
                                // Log the error message as it was received from OpenSearch.
                                logger.debug(e.getMessage());

                                // Return a more descriptive error message for the user.
                                ZentityPlugin.sendResponseError(channel, logger, new ForbiddenException("The '" + ModelsAction.INDEX_NAME + "' index cannot be created. This action requires the 'create_index' privilege for the '" + ModelsAction.INDEX_NAME + "' index. Your role does not have this privilege."));
                            } else {

                                // The error was unexpected.
                                ZentityPlugin.sendResponseError(channel, logger, e);
                            }
                        }
                    });

                } else {
                    throw new NotImplementedException("Method and endpoint not implemented.");
                }
            } catch (NotImplementedException e) {
                channel.sendResponse(new BytesRestResponse(channel, RestStatus.NOT_IMPLEMENTED, e));
            }
        };
    }
}
