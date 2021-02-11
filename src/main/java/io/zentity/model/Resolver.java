package io.zentity.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.zentity.common.Json;
import io.zentity.common.Patterns;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Resolver {

    public static final Set<String> REQUIRED_FIELDS = new TreeSet<>(
            Arrays.asList("attributes")
    );

    private final String name;
    private Set<String> attributes = new TreeSet<>();
    private boolean validateRunnable = false;
    private int weight = 0;

    public Resolver(String name, JsonNode json) throws ValidationException {
        validateName(name);
        this.name = name;
        this.deserialize(json);
    }

    public Resolver(String name, String json) throws ValidationException, IOException {
        validateName(name);
        this.name = name;
        this.deserialize(json);
    }

    public Resolver(String name, JsonNode json, boolean validateRunnable) throws ValidationException {
        validateName(name);
        this.name = name;
        this.validateRunnable = validateRunnable;
        this.deserialize(json);
    }

    public Resolver(String name, String json, boolean validateRunnable) throws ValidationException, IOException {
        validateName(name);
        this.name = name;
        this.validateRunnable = validateRunnable;
        this.deserialize(json);
    }

    public String name() {
        return this.name;
    }

    public Set<String> attributes() {
        return this.attributes;
    }

    public int weight () { return this.weight; }

    public void attributes(JsonNode value) throws ValidationException {
        validateAttributes(value);
        Set<String> attributes = new TreeSet<>();
        for (JsonNode attribute : value)
            attributes.add(attribute.textValue());
        this.attributes = attributes;
    }

    public void weight(JsonNode value) throws ValidationException {
        validateWeight(value);
        this.weight = value.asInt();
    }

    private void validateName(String value) throws ValidationException {
        if (Patterns.EMPTY_STRING.matcher(value).matches())
            throw new ValidationException("'resolvers' has a resolver with an empty name.");
    }

    private void validateAttributes(JsonNode value) throws ValidationException {
        if (!value.isArray())
            throw new ValidationException("'resolvers." + this.name + ".attributes' must be an array of strings.");
        if (value.size() == 0)
            throw new ValidationException("'resolvers." + this.name + ".attributes' must not be empty.");
        for (JsonNode attribute : value) {
            if (!attribute.isTextual())
                throw new ValidationException("'resolvers." + this.name + ".attributes' must be an array of strings.");
            String attributeName = attribute.textValue();
            if (attributeName == null || Patterns.EMPTY_STRING.matcher(attributeName).matches())
                throw new ValidationException("'resolvers." + this.name + ".attributes' must be an array of non-empty strings.");
        }
    }

    private void validateWeight(JsonNode value) throws ValidationException {
        if (!value.isNull() && !value.isInt())
            throw new ValidationException("'resolvers." + this.name + ".weight' must be an integer.");
    }

    private void validateObject(JsonNode object) throws ValidationException {
        if (!object.isObject())
            throw new ValidationException("'resolvers." + this.name + "' must be an object.");
        if (this.validateRunnable) {
            if (object.size() == 0) {
                // Clarifying "in the entity model" because this exception likely will appear only for resolution requests,
                // and the user might think that the message is referring to the input instead of the entity model.
                throw new ValidationException("'resolvers." + this.name + "' must not be empty in the entity model.");
            }
        }
    }

    /**
     * Deserialize, validate, and hold the state of a resolver object of an entity model.
     * Expected structure of the json variable:
     * <pre>
     * {
     *   "attributes": [
     *      ATTRIBUTE_NAME,
     *      ...
     *    ],
     *   "weight": INTEGER
     * }
     * </pre>
     *
     * @param json Resolver object of an entity model.
     * @throws ValidationException
     */
    public void deserialize(JsonNode json) throws ValidationException {
        validateObject(json);

        // Validate the existence of required fields.
        for (String field : REQUIRED_FIELDS)
            if (!json.has(field))
                throw new ValidationException("'resolvers." + this.name + "' is missing required field '" + field + "'.");

        // Validate and hold the state of fields.
        Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String name = field.getKey();
            JsonNode value = field.getValue();
            switch (name) {
                case "attributes":
                    this.attributes(value);
                    break;
                case "weight":
                    this.weight(value);
                    break;
                default:
                    throw new ValidationException("'resolvers." + this.name + "." + name + "' is not a recognized field.");
            }
        }
    }

    public void deserialize(String json) throws ValidationException, IOException {
        deserialize(Json.MAPPER.readTree(json));
    }

}
