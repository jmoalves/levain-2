package com.github.jmoalves.levain.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.jmoalves.levain.util.FileUtils;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Update a JSON file with the provided value at a path.
 *
 * Usage:
 *   - jsonSet <file> [path][to][key] <value>
 */
@ApplicationScoped
public class JsonSetAction implements Action {
    private static final Logger logger = LoggerFactory.getLogger(JsonSetAction.class);
    private static final Pattern PATH_TOKEN_PATTERN = Pattern.compile("\\[([^\\]]+)\\]");

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String name() {
        return "jsonSet";
    }

    @Override
    public void execute(ActionContext context, List<String> args) throws Exception {
        if (context == null) {
            throw new IllegalArgumentException("jsonSet requires an action context");
        }
        if (args == null || args.size() < 3) {
            throw new IllegalArgumentException("jsonSet requires: <file> <path> <value>");
        }

        String fileArg = args.get(0);
        String pathArg = args.get(1);
        String valueArg = args.get(2);

        List<PathToken> tokens = parsePath(pathArg);
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("jsonSet requires a path like [key][0][name]");
        }

        Path filePath = FileUtils.resolve(context.getBaseDir(), fileArg);
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }

        ObjectNode root = mapper.createObjectNode();
        if (Files.exists(filePath)) {
            JsonNode existing = mapper.readTree(filePath.toFile());
            if (existing instanceof ObjectNode) {
                root = (ObjectNode) existing;
            }
        }

        setValue(root, tokens, valueArg);

        mapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), root);
        logger.debug("jsonSet updated {} at {}", filePath, pathArg);
    }

    private List<PathToken> parsePath(String path) {
        List<PathToken> tokens = new ArrayList<>();
        Matcher matcher = PATH_TOKEN_PATTERN.matcher(path);
        while (matcher.find()) {
            String value = matcher.group(1);
            if (value.matches("\\d+")) {
                tokens.add(PathToken.index(Integer.parseInt(value)));
            } else {
                tokens.add(PathToken.key(value));
            }
        }
        return tokens;
    }

    private void setValue(ObjectNode root, List<PathToken> tokens, String valueArg) {
        JsonNode current = root;
        for (int i = 0; i < tokens.size(); i++) {
            PathToken token = tokens.get(i);
            boolean last = i == tokens.size() - 1;
            PathToken next = last ? null : tokens.get(i + 1);

            if (token.isKey()) {
                if (!(current instanceof ObjectNode)) {
                    throw new IllegalArgumentException("Invalid path: expected object for " + token.key);
                }
                ObjectNode obj = (ObjectNode) current;
                if (last) {
                    obj.set(token.key, toValueNode(valueArg));
                } else {
                    JsonNode child = obj.get(token.key);
                    if (child == null || child.isNull()) {
                        child = next != null && next.isIndex() ? mapper.createArrayNode() : mapper.createObjectNode();
                        obj.set(token.key, child);
                    }
                    current = child;
                }
            } else {
                if (!(current instanceof ArrayNode)) {
                    throw new IllegalArgumentException("Invalid path: expected array at index " + token.index);
                }
                ArrayNode array = (ArrayNode) current;
                ensureArraySize(array, token.index);
                if (last) {
                    array.set(token.index, toValueNode(valueArg));
                } else {
                    JsonNode child = array.get(token.index);
                    if (child == null || child.isNull()) {
                        child = next != null && next.isIndex() ? mapper.createArrayNode() : mapper.createObjectNode();
                        array.set(token.index, child);
                    }
                    current = child;
                }
            }
        }
    }

    private JsonNode toValueNode(String value) {
        if (value == null) {
            return TextNode.valueOf("");
        }
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed)) {
            return mapper.getNodeFactory().booleanNode(true);
        }
        if ("false".equalsIgnoreCase(trimmed)) {
            return mapper.getNodeFactory().booleanNode(false);
        }
        return TextNode.valueOf(value);
    }

    private void ensureArraySize(ArrayNode array, int index) {
        while (array.size() <= index) {
            array.addNull();
        }
    }

    private static class PathToken {
        private final String key;
        private final Integer index;

        private PathToken(String key, Integer index) {
            this.key = key;
            this.index = index;
        }

        static PathToken key(String key) {
            return new PathToken(key, null);
        }

        static PathToken index(int index) {
            return new PathToken(null, index);
        }

        boolean isKey() {
            return key != null;
        }

        boolean isIndex() {
            return index != null;
        }
    }
}
