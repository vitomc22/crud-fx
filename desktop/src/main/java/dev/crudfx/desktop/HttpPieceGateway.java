package dev.crudfx.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class HttpPieceGateway implements PieceGateway {
    private static final TypeReference<List<Piece>> PIECE_LIST = new TypeReference<>() {
    };

    private final URI baseUri;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpPieceGateway() {
        this(URI.create(System.getProperty("crudfx.api.url", "http://localhost:8080")));
    }

    public HttpPieceGateway(URI baseUri) {
        this.baseUri = baseUri;
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.httpClient = HttpClient.newBuilder().cookieHandler(cookieManager).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public UserSession login(String email, String password) {
        HttpResponse<String> response = send("POST", "/api/auth/login", new LoginRequest(email, password));
        if (response.statusCode() == 401) {
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }
        requireSuccess(response);
        return read(response.body(), UserSession.class);
    }

    @Override
    public List<Piece> findPieces() {
        HttpResponse<String> response = send("GET", "/api/pieces", null);
        requireSuccess(response);
        return read(response.body(), PIECE_LIST);
    }

    @Override
    public Piece create(Piece piece) {
        HttpResponse<String> response = send("POST", "/api/pieces", piece);
        requireSuccess(response);
        return read(response.body(), Piece.class);
    }

    @Override
    public Piece update(Piece piece) {
        HttpResponse<String> response = send("PUT", pathFor(piece.partNumber()), piece);
        requireSuccess(response);
        return read(response.body(), Piece.class);
    }

    @Override
    public void delete(String partNumber) {
        HttpResponse<String> response = send("DELETE", pathFor(partNumber), null);
        requireSuccess(response);
    }

    @Override
    public void logout() {
        HttpResponse<String> response = send("POST", "/api/auth/logout", null);
        requireSuccess(response);
    }

    private HttpResponse<String> send(String method, String path, Object body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(path))
                .header("Accept", "application/json");
        if (body == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(write(body)));
        }

        try {
            return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("A chamada ao backend foi interrompida.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível conectar ao backend.", exception);
        }
    }

    private void requireSuccess(HttpResponse<String> response) {
        if (response.statusCode() == 401) {
            throw new IllegalStateException("Sessão expirada. Entre novamente.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("O backend respondeu com HTTP " + response.statusCode() + ".");
        }
    }

    private String pathFor(String partNumber) {
        return "/api/pieces/" + URLEncoder.encode(partNumber, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível preparar a solicitação.", exception);
        }
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException exception) {
            throw new IllegalStateException("O backend retornou uma resposta inválida.", exception);
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException exception) {
            throw new IllegalStateException("O backend retornou uma resposta inválida.", exception);
        }
    }

    private record LoginRequest(String email, String password) {
    }
}