package io.wkrzywiec.keycloak.backend.movie;

import com.auth0.jwk.JwkProvider;
import io.restassured.RestAssured;
import io.wkrzywiec.keycloak.backend.infra.security.KeycloakJwkProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class MovieControllerSecurityTest {

    @Container
    private static GenericContainer keycloak = new GenericContainer(DockerImageName.parse("jboss/keycloak:11.0.2"))
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_USER", "admin")
            .withEnv("KEYCLOAK_PASSWORD", "admin")
            .withEnv("DB_VENDOR", "h2")
            .withEnv("KEYCLOAK_IMPORT", "/tmp/realm-test.json")
            .withClasspathResourceMapping("keycloak/realm-test.json", "/tmp/realm-test.json", BindMode.READ_ONLY)
            .withCommand("-Dkeycloak.profile.feature.upload_scripts=enabled")
            .waitingFor(Wait.forHttp("/auth/realms/master"));

    @Autowired
    private MockMvc mockMvc;


    @Test
    @DisplayName("Try to get all movies (request without Authorization header)")
    void requestAllMoviesWithoutAuthorizationHeader() throws Exception {

        mockMvc.perform(
                get("/movies"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Get all movies (request with Authorization header)")
    void getAllMoviesWithAuthorizationHeader() throws Exception {

        String accessToken = fetchAccessToken();

        mockMvc.perform(
                get("/movies")
                        .header("Authorization", "Bearer " + accessToken))
                .andDo(print())
                .andExpect(status().isOk());
    }

    private String fetchAccessToken() {

        String keycloakUrl = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/auth/realms/test";

        Map<String, String> formParams = Map.of(
                "grant_type", "password",
                "scope", "openid",
                "client_id", "backend",
                "client_secret", "372d2f4b-c498-4b7e-a7ac-85c73a26eaad",
                "username", "han",
                "password", "password"
        );

        var response =
                given()
                    .contentType("application/x-www-form-urlencoded")
                    .accept("application/json, text/plain, */*")
                    .formParams(formParams)
                        .log().all()
                .when()
                    .post(keycloakUrl + "/protocol/openid-connect/token")
                        .prettyPeek()
                .then();

        response.statusCode(200);

        return response.extract().body().jsonPath().getString("access_token");
    }

    @Configuration
    private static class TestConfiguration {

        @Bean
        @Primary
        public JwkProvider keycloakJwkProvider() {
            String jwkUrl = "http://" + keycloak.getHost() + ":" + keycloak.getMappedPort(8080) + "/auth/realms/test" + "/protocol/openid-connect/certs";
            return new KeycloakJwkProvider(jwkUrl);
        }
    }
}