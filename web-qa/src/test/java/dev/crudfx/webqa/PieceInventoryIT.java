package dev.crudfx.webqa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PieceInventoryIT {
    private static final String EMAIL = "qa@crudfx.local";
    private static final String PASSWORD = "qa1234";
    private static final String WEB_BASE_URL = System.getProperty("web.base.url", "http://localhost:5173");
    private static final String API_BASE_URL = System.getProperty("api.base.url", "http://localhost:8080");

    private static Playwright playwright;
    private static Browser browser;

    private BrowserContext context;
    private Page page;
    private String partNumberForCleanup;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void openPage() {
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1280, 900));
        page = context.newPage();
        page.navigate(WEB_BASE_URL);
        page.getByTestId("login-button").waitFor();
    }

    @AfterEach
    void removeCreatedPiece() {
        if (partNumberForCleanup != null && context != null) {
            APIRequestContext request = context.request();
            APIResponse response = request.delete(API_BASE_URL + "/api/pieces/"
                    + URLEncoder.encode(partNumberForCleanup, StandardCharsets.UTF_8).replace("+", "%20"));
            assertTrue(response.status() == 204 || response.status() == 404,
                    "A limpeza E2E deve excluir o registro de teste: HTTP " + response.status());
        }
        if (context != null) context.close();
    }

    @Test
    void rejectsInvalidLogin() {
        assertTrue(page.getByRole(com.microsoft.playwright.options.AriaRole.HEADING,new Page.GetByRoleOptions().setName("Acesse o inventário")).isVisible());
        page.getByTestId("email-field").fill(EMAIL);
        page.getByTestId("password-field").fill("wrong-password");
        page.getByTestId("login-button").click();

        page.getByText("E-mail ou senha inválidos.", new Page.GetByTextOptions().setExact(true)).waitFor();
        assertEquals("E-mail ou senha inválidos.", page.getByTestId("status-message").innerText());
        assertTrue(page.getByTestId("login-button").isVisible());
    }

    @Test
    void logsInAndFiltersPieces() {
        login();
        String partNumber = newPartNumber();
        createPiece(partNumber, "Filtro Playwright");

        page.getByTestId("search-field").fill(partNumber);
        assertTrue(page.getByTestId("piece-row-" + partNumber).isVisible());

        page.getByTestId("search-field").fill("sem-resultado");
        assertFalse(page.getByTestId("piece-row-" + partNumber).isVisible());

        page.getByTestId("search-field").fill("Registro criado pelo cenário Playwright");
        assertFalse(page.getByTestId("piece-row-" + partNumber).isVisible());
    }

    @Test
    void createsEditsAndDeletesPiece() {
        login();
        assertTrue(page.getByRole(com.microsoft.playwright.options.AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Dados da peça")).isVisible());
        assertEquals("Nova peça", page.getByTestId("new-piece-button").innerText());
        assertEquals("Salvar peça", page.getByTestId("save-piece-button").innerText());
        assertTrue(page.getByTestId("delete-piece-button").isDisabled());
        assertEquals("Descrição opcional", page.getByTestId("description-field").getAttribute("placeholder"));

        String partNumber = newPartNumber();
        createPiece(partNumber, "Suporte original");

        page.getByTestId("edit-" + partNumber).click();
        assertEquals(Boolean.TRUE,
            page.getByTestId("part-number-field").evaluate("element => element.readOnly"));
        assertTrue(page.getByTestId("delete-piece-button").isEnabled());
        page.getByTestId("new-piece-button").click();
        assertEquals("", page.getByTestId("part-number-field").inputValue());
        assertTrue(page.getByTestId("delete-piece-button").isDisabled());

        page.getByTestId("edit-" + partNumber).click();
        page.getByTestId("name-field").fill("Suporte revisado");
        page.getByTestId("save-piece-button").click();

        page.getByText("Peça atualizada.", new Page.GetByTextOptions().setExact(true)).waitFor();
        assertTrue(page.getByTestId("piece-row-" + partNumber).innerText().contains("Suporte revisado"));
        assertEquals("Peça atualizada.", page.getByTestId("status-message").innerText());

        page.getByTestId("edit-" + partNumber).click();
        page.getByTestId("delete-piece-button").click();
        page.getByText("Peça excluída.", new Page.GetByTextOptions().setExact(true)).waitFor();
        assertFalse(page.getByTestId("piece-row-" + partNumber).isVisible());
        assertEquals("Peça excluída.", page.getByTestId("status-message").innerText());
        partNumberForCleanup = null;
    }

    @Test
    void requiresMandatoryPieceFields() {
        login();
        page.getByTestId("save-piece-button").click();

        assertEquals(Boolean.TRUE,
            page.getByTestId("part-number-field").evaluate("element => element.validity.valueMissing"));
        assertEquals(Boolean.TRUE,
            page.getByTestId("name-field").evaluate("element => element.validity.valueMissing"));
        assertEquals(Boolean.TRUE,
            page.getByTestId("revision-field").evaluate("element => element.validity.valueMissing"));
    }

    @Test
    void logoutInvalidatesPieceSession() {
        login();
        APIResponse authenticatedResponse = context.request().get(API_BASE_URL + "/api/pieces");
        assertEquals(200, authenticatedResponse.status());

        page.getByTestId("logout-button").click();
        page.getByTestId("login-button").waitFor();
        assertTrue(page.getByTestId("login-button").isVisible());
        assertEquals(401, context.request().get(API_BASE_URL + "/api/pieces").status());
    }

    @Test
    void captureDocumentationScreenshots() {
        if (!Boolean.getBoolean("capture.documentation.screenshots")) return;
        Path screenshotDirectory = Path.of(System.getProperty("screenshot.output.dir", "docs/screenshots"));
        try {
            Files.createDirectories(screenshotDirectory);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Não foi possível criar o diretório das capturas.", exception);
        }
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(screenshotDirectory.resolve("web-login.png"))
                .setFullPage(true));

        login();
        createPiece(newPartNumber(), "Suporte web de demonstração");
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(screenshotDirectory.resolve("web-inventory.png"))
                .setFullPage(true));
    }

    private void login() {
        page.getByTestId("email-field").fill(EMAIL);
        page.getByTestId("password-field").fill(PASSWORD);
        page.getByTestId("login-button").click();
        page.getByRole(com.microsoft.playwright.options.AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("Inventário de peças")).waitFor();
    }

    private void createPiece(String partNumber, String name) {
        partNumberForCleanup = partNumber;
        page.getByTestId("part-number-field").fill(partNumber);
        page.getByTestId("name-field").fill(name);
        page.getByTestId("revision-field").fill("A");
        page.getByTestId("description-field").fill("Registro criado pelo cenário Playwright");
        page.getByTestId("save-piece-button").click();
        page.getByTestId("piece-row-" + partNumber).waitFor(
                new com.microsoft.playwright.Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
    }

    private String newPartNumber() {
        return "PW-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }
}