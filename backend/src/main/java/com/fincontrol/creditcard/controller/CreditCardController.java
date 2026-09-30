package com.fincontrol.creditcard.controller;

import com.fincontrol.creditcard.dto.CreditCardDtos;
import com.fincontrol.creditcard.service.CreditCardService;
import com.fincontrol.shared.security.CurrentUser;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/credit-cards")
@SecurityRequirement(name = "bearerAuth")
public class CreditCardController {
    private final CreditCardService service;
    private final CurrentUser currentUser;

    public CreditCardController(CreditCardService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<CreditCardDtos.Response> list() {
        return service.list(currentUser.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreditCardDtos.Response create(@Valid @RequestBody CreditCardDtos.Request request) {
        return service.create(currentUser.id(), request);
    }

    @PutMapping("/{cardId}")
    public CreditCardDtos.Response update(@PathVariable UUID cardId, @Valid @RequestBody CreditCardDtos.Request request) {
        return service.update(currentUser.id(), cardId, request);
    }

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID cardId) {
        service.delete(currentUser.id(), cardId);
    }

    @GetMapping("/{cardId}/invoices")
    public CreditCardDtos.InvoiceResponse invoice(@PathVariable UUID cardId, @RequestParam String month) {
        return service.invoice(currentUser.id(), cardId, month);
    }

    @PostMapping("/{cardId}/invoices/{invoiceId}/payments")
    public CreditCardDtos.InvoiceResponse pay(@PathVariable UUID cardId, @PathVariable UUID invoiceId,
                                              @Valid @RequestBody CreditCardDtos.PaymentRequest request) {
        return service.pay(currentUser.id(), cardId, invoiceId, request);
    }
}
