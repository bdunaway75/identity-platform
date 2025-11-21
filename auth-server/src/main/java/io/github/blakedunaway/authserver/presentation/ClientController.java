package io.github.blakedunaway.authserver.presentation;

import com.blakedunaway.iamclientapi.api.dto.RegisteredClientDto;
import io.github.blakedunaway.authserver.business.service.api.RegisteredClientApiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients", description = "Client registration & OAuth flows")
public class ClientController {

    private final RegisteredClientApiService service;

    @Operation(summary = "Register a client")
    @PostMapping
    public ResponseEntity<?> register(@RequestBody final RegisteredClientDto dto) {
        final Response jaxrs = service.registerClient(dto);
        return ResponseEntity.status(jaxrs.getStatus()).body(jaxrs.getEntity());
    }
}
