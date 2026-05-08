package com.example.userserviceejb.controller;

import com.example.userserviceejb.dto.LoginRequest;
import com.example.userserviceejb.dto.RegisterCustomerRequest;
import com.example.userserviceejb.dto.RegisterProviderRequest;
import com.example.userserviceejb.dto.TokenValidationResponse;
import com.example.userserviceejb.dto.WalletDeductRequest;
import com.example.userserviceejb.dto.WalletRequest;
import com.example.userserviceejb.entity.User;
import com.example.userserviceejb.service.AuthServiceBean;
import com.example.userserviceejb.service.JwtServiceBean;
import com.example.userserviceejb.service.UserServiceBean;
import com.example.userserviceejb.service.UserServiceBean.UserNotFoundException;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.math.BigDecimal;
import java.util.Map;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

    @Inject
    private UserServiceBean userServiceBean;

    @Inject
    private AuthServiceBean authServiceBean;

    @Inject
    private JwtServiceBean jwtServiceBean;

    @POST
    @Path("/register/customer")
    public Response registerCustomer(RegisterCustomerRequest request) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(userServiceBean.registerCustomer(request))
                    .build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @POST
    @Path("/register/provider")
    public Response registerProvider(RegisterProviderRequest request) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(userServiceBean.registerProvider(request))
                    .build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        return authServiceBean.login(request)
                .map(loginResponse -> Response.ok(loginResponse).build())
                .orElseGet(() -> Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "Invalid username or password"))
                        .build());
    }

    @GET
    @Path("/token/validate")
    public Response validateToken(@HeaderParam("Authorization") String authorizationHeader) {
        return jwtServiceBean.validateToken(authorizationHeader)
                .map(claims -> Response.ok(new TokenValidationResponse(
                        "Token is valid",
                        claims.getUserId(),
                        claims.getUsername(),
                        claims.getRole(),
                        claims.getProfessionType()
                )).build())
                .orElseGet(() -> Response.status(Response.Status.UNAUTHORIZED)
                        .entity(Map.of("message", "Invalid or expired token"))
                        .build());
    }

    @GET
    public Response getAllUsers() {
        return Response.ok(userServiceBean.getAllUsers()).build();
    }

    @GET
    @Path("/{id}")
    public Response getUserById(@PathParam("id") Long id) {
        return userServiceBean.getUserById(id)
                .map(user -> Response.ok(user).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "User not found"))
                        .build());
    }

    @POST
    public Response addUser(User user) {
        try {
            return Response.status(Response.Status.CREATED)
                    .entity(userServiceBean.addUser(user))
                    .build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @POST
    @Path("/wallet/add/{id}")
    public Response addFunds(@PathParam("id") Long id, WalletRequest request) {
        try {
            BigDecimal amount = request == null ? null : request.getAmount();
            return userServiceBean.addFunds(id, amount)
                    .map(user -> Response.ok(Map.of(
                            "message", "Funds added successfully",
                            "userId", user.getId(),
                            "balance", user.getBalance()
                    )).build())
                    .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                            .entity(Map.of("message", "User not found"))
                            .build());
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @GET
    @Path("/wallet/{id}")
    public Response getWalletBalance(@PathParam("id") Long id) {
        return userServiceBean.getWalletBalance(id)
                .map(balance -> Response.ok(Map.of("userId", id, "balance", balance)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND)
                        .entity(Map.of("message", "User not found"))
                        .build());
    }

    @POST
    @Path("/wallet/deduct/{userId}")
    public Response deductWallet(@PathParam("userId") Long userId, WalletDeductRequest request) {
        try {
            double amount = request == null ? 0 : request.getAmount();
            double newBalance = userServiceBean.deductWallet(userId, amount);
            return Response.ok(Map.of(
                    "message", "Wallet deducted successfully",
                    "newBalance", newBalance
            )).build();
        } catch (UserNotFoundException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", exception.getMessage()))
                    .build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    @POST
    @Path("/wallet/refund/{userId}")
    public Response refundWallet(@PathParam("userId") Long userId, WalletDeductRequest request) {
        try {
            double amount = request == null ? 0 : request.getAmount();
            double newBalance = userServiceBean.refundWallet(userId, amount);
            return Response.ok(Map.of(
                    "message", "Wallet refunded successfully",
                    "newBalance", newBalance
            )).build();
        } catch (UserNotFoundException exception) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("message", exception.getMessage()))
                    .build();
        } catch (IllegalArgumentException exception) {
            return badRequest(exception.getMessage());
        }
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Map.of("message", message))
                .build();
    }
}
