package ru.yandex.practicum.frontui.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.yandex.practicum.frontui.client.dto.AccountResponse;
import ru.yandex.practicum.frontui.client.dto.AccountShortResponse;
import ru.yandex.practicum.frontui.client.dto.UpdateAccountRequest;
import ru.yandex.practicum.frontui.client.dto.CashRequest;
import ru.yandex.practicum.frontui.client.dto.TransferRequest;
import ru.yandex.practicum.frontui.controller.dto.CashAction;
import ru.yandex.practicum.frontui.service.FrontService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class MainController {

    private final FrontService frontService;

    @GetMapping
    public String index() {
        return "redirect:/account";
    }

    @GetMapping("/account")
    public String getAccount(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient,
            Model model
    ) {
        fillModel(model, authorizedClient, null, null);
        return "main";
    }

    @PostMapping("/account")
    public String editAccount(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient,
            @RequestParam("name") String name,
            @RequestParam("birthdate") LocalDate birthdate,
            Model model
    ) {
        try {
            frontService.updateAccount(authorizedClient, new UpdateAccountRequest(name, birthdate));
            fillModel(model, authorizedClient, null, "Данные аккаунта обновлены");
        } catch (Exception ex) {
            fillModel(model, authorizedClient, List.of(ex.getMessage()), null);
        }
        return "main";
    }

    @PostMapping("/cash")
    public String editCash(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient,
            @RequestParam("value") int value,
            @RequestParam("action") CashAction action,
            Model model
    ) {
        try {
            String info = frontService.processCash(authorizedClient, new CashRequest(value), action);
            fillModel(model, authorizedClient, null, info);
        } catch (Exception ex) {
            fillModel(model, authorizedClient, List.of(ex.getMessage()), null);
        }
        return "main";
    }

    @PostMapping("/transfer")
    public String transfer(
            @RegisteredOAuth2AuthorizedClient("keycloak") OAuth2AuthorizedClient authorizedClient,
            @RequestParam("value") int value,
            @RequestParam("login") String login,
            Model model
    ) {
        try {
            String info = frontService.transfer(authorizedClient,
                    new TransferRequest(login, value));
            fillModel(model, authorizedClient, null, info);
        } catch (Exception ex) {
            fillModel(model, authorizedClient, List.of(ex.getMessage()), null);
        }
        return "main";
    }

    private void fillModel(Model model,
                           OAuth2AuthorizedClient authorizedClient,
                           List<String> errors,
                           String info) {
        AccountResponse account = frontService.getMyAccount(authorizedClient);
        List<AccountShortResponse> others = frontService.getOtherAccounts(authorizedClient);
        model.addAttribute("name", account.name());
        model.addAttribute("birthdate", account.birthdate().format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("sum", account.balance());
        model.addAttribute("accounts", others);
        model.addAttribute("errors", errors);
        model.addAttribute("info", info);
    }
}
