package com.innoshare.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.innoshare.model.domain.User;
import com.innoshare.model.request.UserRequest;
import com.innoshare.service.impl.UserServiceImpl;

import com.innoshare.common.Response;
import com.innoshare.utils.CookieUtil;
import com.innoshare.utils.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

@RestController
@RequestMapping("user")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userServiceImpl;

    @PostMapping("add")
    public Response addUser(@RequestBody UserRequest userRequest) {
        return userServiceImpl.addUser(userRequest);
    }

    @GetMapping("login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        Response response = userServiceImpl.getUserWithPassword(username, password);
        User user = (User)response.getData();
        if(response.getSuccess()){
            HashMap<String, String> payload = new HashMap<>();
            payload.put("username", user.getUsername());
            payload.put("userId", String.valueOf(user.getUserId()));
            try{
                String token= JWTUtil.generateToken(payload);
                ResponseCookie cookie = ResponseCookie
                        .from("token", token)
                        .httpOnly(true)
                        .secure(true)
                        .path("/")
                        .maxAge(3600)
                        .build();
                return ResponseEntity.ok().header("Set-Cookie", cookie.toString()).body(response);
            }catch (Exception e){
                System.out.println(e.getMessage());
                return ResponseEntity.internalServerError().build();
            }
        }
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("changePassword")
    public Response changePassword(@RequestParam String oldPassword, @RequestParam String newPassword, HttpServletRequest request) {
        String token = CookieUtil.getCookie(request, "token");
        if(token == null){
            return Response.warning("请重新登录");
        }
        try{
            int userId=JWTUtil.getUserId(token);
            return userServiceImpl.updateUserPassword(userId, oldPassword, newPassword);
        }catch (UnsupportedEncodingException e){
            return Response.fatal("JWT解码故障");
        }

    }
}