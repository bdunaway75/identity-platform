package io.github.blakedunaway.authserviceclient.service;

import io.github.blakedunaway.authserviceclient.dto.RegisteredClientDto;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("register-client")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public interface RegisterClientApi {

    @POST
    @Path("/register")
    Response registerClient(final RegisteredClientDto registeredClientDto);

    @POST
    @Path("/update")
    Response updateClient(final RegisteredClientDto registeredClientDto);

}
