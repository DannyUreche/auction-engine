package com.DannyU.auction_engine.config;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// The Swagger documentation is complete (summary + tag everywhere) and its padlocks tell the truth
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @SuppressWarnings("unchecked")
    void everyOperationIsDocumentedAndPublicOnesWorkWithoutToken() throws Exception {
        String json = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(json, "$.paths");
        assertThat(paths).hasSizeGreaterThan(80);
        assertThat((String) JsonPath.read(json, "$.tags[0].name")).isEqualTo("Autentificare");
        assertThat((List<String>) JsonPath.read(json, "$.tags[*].name")).containsAll(OpenApiConfig.TAG_ORDER);

        List<String> undocumented = new ArrayList<>();
        List<String> publicGets = new ArrayList<>();
        paths.forEach((path, operations) -> operations.forEach((method, operation) -> {
            if (operation.get("summary") == null || operation.get("tags") == null) {
                undocumented.add(method.toUpperCase() + " " + path);
            }
            List<Object> security = (List<Object>) operation.get("security");
            if (method.equals("get") && security != null && security.isEmpty() && !path.contains("{") && !path.equals("/api/events")) {
                publicGets.add(path);
            }
        }));
        assertThat(undocumented).as("operations without summary or tag").isEmpty();
        assertThat(publicGets).contains("/api/items/search", "/api/market", "/api/categories");

        // Swagger says "no token needed": the server must agree
        for (String path : publicGets) {
            int status = mockMvc.perform(get(path)).andReturn().getResponse().getStatus();
            assertThat(status).as(path).isNotIn(401, 403);
        }
        // and a private one keeps its padlock
        Map<String, Object> placeBid = JsonPath.read(json, "$.paths['/api/bids'].post");
        assertThat(placeBid).doesNotContainKey("security"); // inherits the global bearer requirement
        assertThat((Object) JsonPath.read(json, "$.paths['/api/bids'].post.responses['401']")).isNotNull();
    }

    @Test
    void publicRulesFollowTheFilterChainOrder() {
        assertThat(SecurityConfig.isPublic("GET", "/api/items/{id}")).isTrue();
        assertThat(SecurityConfig.isPublic("GET", "/api/items/my")).isFalse();
        assertThat(SecurityConfig.isPublic("POST", "/api/items/{id}/presence")).isTrue();
        assertThat(SecurityConfig.isPublic("POST", "/api/bids")).isFalse();
        assertThat(SecurityConfig.isPublic("POST", "/api/auth/login")).isTrue();
        assertThat(SecurityConfig.isPublic("GET", "/api/admin/stats")).isFalse();
        assertThat(SecurityConfig.isPublic("GET", "/api/collections/mine")).isFalse();
        assertThat(SecurityConfig.isPublic("GET", "/api/collections/{id}")).isTrue();
    }
}
