package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.repository.UserRepository;
import com.shepherdmoney.interviewproject.vo.request.CreateUserPayload;

import java.net.http.HttpResponse.BodyHandler;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.shepherdmoney.interviewproject.model.User;

@RestController
public class UserController {

    // TODO: wire in the user repository (~ 1 line)
     @Autowired
     private UserRepository userRepository;

    @PutMapping("/user")
    public ResponseEntity<Integer> createUser(@RequestBody CreateUserPayload payload) {    

        // TODO: Create an user entity with information given in the payload, store it in the database
        //       and return the id of the user in 200 OK response

        // Create an instance of new user with payload information
        User user = new User();
        user.setName(payload.getName());
        user.setEmail(payload.getEmail());

        // Save the instance to database
        userRepository.save(user);

        // Return HTTP response
        return new ResponseEntity<>(user.getId(), HttpStatus.OK);
    }

    @DeleteMapping("/user")
    public ResponseEntity<String> deleteUser(@RequestParam int userId) {

        // TODO: Return 200 OK if a user with the given ID exists, and the deletion is successful
        //       Return 400 Bad Request if a user with the ID does not exist
        //       The response body could be anything you consider appropriate
        if (!userRepository.existsById(userId)) 
            {
                return new ResponseEntity<>("Invalid ID. User with ID: " + userId + " not found." , HttpStatus.BAD_REQUEST);
            }

        else
            {
                User userToDelete = userRepository.getReferenceById(userId);
                userRepository.delete(userToDelete);
                return new ResponseEntity<>("User with ID: " + userId + " was successfully deleted!", HttpStatus.OK);
            }

    }
}

