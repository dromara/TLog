package com.yomahub.tlog.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JacksonUtil工具类
 *
 * @author iwinkfc@dromara.org
 * @since 1.2.5
 */
public class JacksonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JacksonUtil.class);

    private JacksonUtil() {
    }

    private static class MapperHolder {
        private static final JsonMapper JSON_MAPPER = new JsonMapper();
    }

    public static JsonMapper getMapperInstance() {
        return MapperHolder.JSON_MAPPER;
    }

    public static JsonMapper getMapperInstance(boolean createNew) {
        if (createNew) {
            return new JsonMapper();
        }
        return getMapperInstance();
    }

    public static String toJson(Object o) {
        try {
            return getMapperInstance().writeValueAsString(o);
        } catch (IOException e) {
            logger.warn("write to json string error:{},{}", o, e);
        }
        return null;
    }

    public static JsonNode fromJson(String jsonString) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }
        try {
            JsonNode vo = getMapperInstance().readValue(jsonString, JsonNode.class);
            return vo;
        } catch (IOException e) {
            logger.warn("parse json string error:{},{}", jsonString, e);
        }
        return null;
    }

    public static <T> T fromJson(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }
        try {
            T vo = getMapperInstance().readValue(jsonString, clazz);
            return vo;
        } catch (IOException e) {
            logger.warn("parse json string error:{},{}", jsonString, e);
        }
        return null;
    }


    public static <T> T fromJson(String jsonString, JavaType javaType) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        } else {
            try {
                return getMapperInstance().readValue(jsonString, javaType);
            } catch (IOException var4) {
                logger.warn("parse json string error:{}", new Object[]{jsonString, var4});
                return null;
            }
        }
    }

    public static JavaType createCollectionType(Class<?> collectionClass, Class<?>[] elementClasses) {
        return getMapperInstance().getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }

    public static <T> T update(String jsonString, T object) {
        try {
            return getMapperInstance().readerForUpdating(object).readValue(jsonString);
        } catch (JsonProcessingException var4) {
            logger.warn("update json string:{} to object error.", new Object[]{jsonString, object, var4});
        } catch (IOException var5) {
            logger.warn("update json string:{} to object: error.", new Object[]{jsonString, object, var5});
        }

        return null;
    }

    public void enableEnumUseToString() {
        getMapperInstance().enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        getMapperInstance().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    }

    public ObjectMapper getMapper() {
        return getMapperInstance();
    }
}
