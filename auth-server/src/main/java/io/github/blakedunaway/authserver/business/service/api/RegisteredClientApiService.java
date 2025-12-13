package io.github.blakedunaway.authserver.business.service.api;

import io.github.blakedunaway.authserver.business.model.RegisteredClientModel;
import io.github.blakedunaway.authserver.business.service.RegisteredClientService;
import io.github.blakedunaway.authserver.business.validation.ClientRegistrationValidator;
import io.github.blakedunaway.authserver.mapper.RegisteredClientMapper;
import io.github.blakedunaway.authserviceclient.dto.RegisteredClientDto;
import io.github.blakedunaway.authserviceclient.service.RegisterClientApi;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class RegisteredClientApiService implements RegisterClientApi {

    private static final Logger log = LoggerFactory.getLogger(RegisteredClientApiService.class);

    private final RegisteredClientMapper registeredClientMapper;

    private final RegisteredClientService registeredClientService;

    @Override
    public Response registerClient(final RegisteredClientDto dto) {
        try {
            final Map<String, List<String>> errors = ClientRegistrationValidator.isValid(dto);
            if (errors.isEmpty()) {
                final RegisteredClientModel saved =
                        registeredClientService.saveRegisteredClient(registeredClientMapper.registeredClientDtoToRegisteredClientModel(dto));

                final String id = saved.getClientId();
                final URI location = URI.create("/clients/" + id);
                return Response.created(location)
                               .type(MediaType.APPLICATION_JSON_TYPE)
                               .entity(saved)
                               .build();
            }
            else  {
                return problem(Status.BAD_REQUEST, Status.BAD_REQUEST.getReasonPhrase(), "Invalid client", Map.of("errors", errors));
            }
        } catch (ValidationException e) {
            return problem(Status.BAD_REQUEST, Status.BAD_REQUEST.getReasonPhrase(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return problem(Status.BAD_REQUEST, Status.BAD_REQUEST.getReasonPhrase(), "Invalid request payload");
        } catch (EntityExistsException e) {
            return problem(Status.CONFLICT, Status.CONFLICT.getReasonPhrase(), "Client already exists");
        } catch (Exception e) {
            log.error("registerClient failed", e);
            return problem(Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Unexpected error");
        }
    }

    @Override
    public Response updateClient(final RegisteredClientDto dto) {
        try {
            final Map<String, List<String>> errors = ClientRegistrationValidator.isValid(dto);
            if (errors.isEmpty()) {
                final RegisteredClientModel updated =
                        registeredClientService.updateRegisteredClient(registeredClientMapper.registeredClientDtoToRegisteredClientModel(dto));
                return Response.ok()
                               .type(MediaType.APPLICATION_JSON_TYPE)
                               .entity(updated)
                               .build();
            }
            else  {
                return problem(Status.BAD_REQUEST, Status.BAD_REQUEST.getReasonPhrase(), "Invalid client", Map.of("errors", errors));
            }
        } catch (ValidationException e) {
            return problem(Status.BAD_REQUEST, Status.BAD_REQUEST.getReasonPhrase(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return problem(Status.BAD_REQUEST, Status.BAD_REQUEST.getReasonPhrase(), "Invalid request payload");
        } catch (EntityNotFoundException e) {
            return problem(Status.NOT_FOUND, Status.NOT_FOUND.getReasonPhrase(), "Client does not exist");
        } catch (Exception e) {
            log.error("registerClient failed", e);
            return problem(Status.INTERNAL_SERVER_ERROR, Status.INTERNAL_SERVER_ERROR.getReasonPhrase(), "Unexpected error");
        }
    }

    private Response problem(final Status status, final String code, final String message) {
        return problem(status, code, message, Map.of());
    }

    private Response problem(final Status status, final String code, final String message, final Map<String, Object> extras) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("message", message);
        if (extras != null && !extras.isEmpty()) {
            body.putAll(extras);
        }
        return Response.status(status)
                       .type(MediaType.APPLICATION_JSON_TYPE)
                       .entity(body)
                       .build();
    }

}
