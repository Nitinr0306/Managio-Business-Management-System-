package com.managio.platform.config;

import com.managio.platform.tenancy.BusinessContext;
import com.managio.platform.web.Nullable;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.customizers.PropertyCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.Annotation;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The OpenAPI document is the contract the frontend's TypeScript types are generated from, so it must say
 * exactly what the API does:
 * <ul>
 *   <li>{@link BusinessContext} is resolved on the server, so it is not a request parameter;</li>
 *   <li>every path under {@code /businesses/{businessId}} takes that path parameter, even though the
 *       controllers leave reading it to the tenancy interceptor;</li>
 *   <li>response objects always contain every property (Jackson writes nulls), so their properties are
 *       required, and only fields marked {@link Nullable} may be null.</li>
 * </ul>
 */
@Configuration
class OpenApiConfig {

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(BusinessContext.class);
    }

    private static final String SCHEMA_REF = "#/components/schemas/";

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("Managio API").version("v1")
                        .description("Multi-business management platform. Business-scoped resources live under /api/v1/businesses/{businessId}."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }

    /** Publishes fields marked {@link Nullable} as "value or null". */
    @Bean
    PropertyCustomizer nullableProperties() {
        return (property, type) -> {
            Annotation[] annotations = type.getCtxAnnotations();
            if (annotations == null || java.util.Arrays.stream(annotations).noneMatch(a -> a instanceof Nullable)) {
                return property;
            }
            if (property.get$ref() != null) {
                Schema<Object> wrapper = new Schema<>();
                wrapper.setAnyOf(List.of(new Schema<>().$ref(property.get$ref()), nullSchema()));
                return wrapper;
            }
            Set<String> types = property.getTypes() == null ? new java.util.LinkedHashSet<>() : new java.util.LinkedHashSet<>(property.getTypes());
            if (property.getType() != null) {
                types.add(property.getType());
            }
            types.add("null");
            property.setTypes(types);
            return property;
        };
    }

    @Bean
    OpenApiCustomizer contractCustomizer() {
        return openApi -> {
            addBusinessIdParameter(openApi);
            requireResponseProperties(openApi);
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().remove(BusinessContext.class.getSimpleName());
            }
        };
    }

    private static void addBusinessIdParameter(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, item) -> {
            if (!path.contains("{businessId}")) {
                return;
            }
            for (Operation operation : item.readOperations()) {
                List<Parameter> parameters = operation.getParameters() == null ? new ArrayList<>() : new ArrayList<>(operation.getParameters());
                boolean declared = parameters.stream().anyMatch(p -> "businessId".equals(p.getName()) && "path".equals(p.getIn()));
                if (!declared) {
                    parameters.addFirst(new Parameter().name("businessId").in("path").required(true)
                            .schema(new IntegerSchema().format("int64")));
                    operation.setParameters(parameters);
                }
            }
        });
    }

    /** Schemas used only in responses get every property marked required. */
    private static void requireResponseProperties(OpenAPI openApi) {
        if (openApi.getPaths() == null || openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        Map<String, Schema> schemas = openApi.getComponents().getSchemas();
        Set<String> requestRoots = new HashSet<>();
        Set<String> responseRoots = new HashSet<>();
        for (PathItem item : openApi.getPaths().values()) {
            for (Operation operation : item.readOperations()) {
                if (operation.getRequestBody() != null) {
                    collectRefs(operation.getRequestBody().getContent(), requestRoots);
                }
                if (operation.getParameters() != null) {
                    operation.getParameters().forEach(p -> collectRefs(p.getSchema(), requestRoots));
                }
                if (operation.getResponses() != null) {
                    operation.getResponses().values().forEach(r -> collectRefs(r.getContent(), responseRoots));
                }
            }
        }
        Set<String> requestSchemas = reachable(requestRoots, schemas);
        for (String name : reachable(responseRoots, schemas)) {
            Schema<?> schema = schemas.get(name);
            if (requestSchemas.contains(name) || schema == null || schema.getProperties() == null) {
                continue;
            }
            schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
        }
    }

    private static Set<String> reachable(Set<String> roots, Map<String, Schema> schemas) {
        Set<String> seen = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            String name = queue.pop();
            if (!seen.add(name)) {
                continue;
            }
            Set<String> refs = new HashSet<>();
            collectRefs(schemas.get(name), refs);
            refs.stream().filter(r -> !seen.contains(r)).forEach(queue::push);
        }
        return seen;
    }

    private static void collectRefs(Content content, Set<String> into) {
        if (content != null) {
            content.values().stream().map(MediaType::getSchema).forEach(s -> collectRefs(s, into));
        }
    }

    private static void collectRefs(Schema<?> schema, Set<String> into) {
        if (schema == null) {
            return;
        }
        if (schema.get$ref() != null && schema.get$ref().startsWith(SCHEMA_REF)) {
            into.add(schema.get$ref().substring(SCHEMA_REF.length()));
        }
        collectRefs(schema.getItems(), into);
        if (schema.getAdditionalProperties() instanceof Schema<?> additional) {
            collectRefs(additional, into);
        }
        for (Collection<Schema> group : java.util.Arrays.<Collection<Schema>>asList(
                schema.getProperties() == null ? null : schema.getProperties().values(),
                schema.getAllOf(), schema.getAnyOf(), schema.getOneOf())) {
            if (group != null) {
                group.stream().filter(Objects::nonNull).forEach(s -> collectRefs(s, into));
            }
        }
    }

    private static Schema<Object> nullSchema() {
        Schema<Object> schema = new Schema<>();
        schema.setTypes(Set.of("null"));
        return schema;
    }
}
