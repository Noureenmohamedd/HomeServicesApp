package com.example.userserviceejb.controller;

import com.example.userserviceejb.dto.RegisterAdminRequest;
import com.example.userserviceejb.entity.UserRole;
import com.example.userserviceejb.service.JwtServiceBean;
import com.example.userserviceejb.service.UserServiceBean;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.function.Supplier;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminController {

    @Inject
    private JwtServiceBean jwtServiceBean;

    @Inject
    private UserServiceBean userServiceBean;

    @POST
    @Path("/register")
    public Response registerAdmin(RegisterAdminRequest request) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(userServiceBean.registerAdmin(request))
                    .build();
        } catch (IllegalArgumentException exception) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("message", exception.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/users")
    public Response getAllUsers(@HeaderParam("Authorization") String authorizationHeader) {
        return adminOnly(authorizationHeader, () -> Response.ok(userServiceBean.getAllUsers()).build());
    }

    @GET
    @Path("/users/registered")
    public Response getRegisteredUsers(@HeaderParam("Authorization") String authorizationHeader) {
        return adminOnly(authorizationHeader, () -> Response.ok(userServiceBean.getRegisteredUsers()).build());
    }

    @GET
    @Path("/users/admins")
    public Response getAdminUsers(@HeaderParam("Authorization") String authorizationHeader) {
        return adminOnly(authorizationHeader, () -> Response.ok(userServiceBean.getAdminUsers()).build());
    }

    @GET
    @Path("/transactions")
    public Response getAllTransactionRecords(@HeaderParam("Authorization") String authorizationHeader) {
        return adminOnly(authorizationHeader, () -> Response.ok(userServiceBean.getAllTransactionRecords()).build());
    }

    private Response adminOnly(String authorizationHeader, Supplier<Response> responseSupplier) {
        return jwtServiceBean.validateToken(authorizationHeader)
                .map(claims -> {
                    if (claims.getRole() != UserRole.ADMIN) {
                        return Response.status(Response.Status.FORBIDDEN)
                                .entity(Map.of("message", "Admin role is required"))
                                .build();
                    }
                    return responseSupplier.get();
                })
                .orElseGet(() -> Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "Missing or invalid token"))
                        .build());
    }
}
