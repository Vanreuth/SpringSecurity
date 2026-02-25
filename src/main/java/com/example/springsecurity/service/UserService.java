package com.example.springsecurity.service;

import com.example.springsecurity.request.UserRequest;
import com.example.springsecurity.response.ApiResponse;
import com.example.springsecurity.response.PageResponse;
import com.example.springsecurity.response.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

  ApiResponse<UserResponse> createUser(UserRequest userRequest , MultipartFile profilePicture);
  PageResponse<UserResponse> getAllUsers(Pageable pageable);
  ApiResponse<UserResponse> getUserById(Long id);
  ApiResponse<UserResponse> findById(String id);
  ApiResponse<UserResponse> updateUser(UserRequest userRequest);

}
