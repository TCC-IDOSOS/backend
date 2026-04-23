package com.core.fisioaging.controller;

import com.core.fisioaging.data.TestUploadData;
import com.core.fisioaging.data.TestUploadResponseData;
import com.core.fisioaging.service.TestUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class TestUploadController {

    private final TestUploadService testUploadService;

    public TestUploadController(TestUploadService testUploadService) {
        this.testUploadService = testUploadService;
    }

    @PostMapping("/tests")
    public ResponseEntity<?> uploadTest(
            @RequestParam("email") String email,
            @RequestBody TestUploadData data) {
        try {
            TestUploadResponseData response = testUploadService.uploadTest(email, data);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            TestUploadResponseData response = new TestUploadResponseData();
            response.setUserEmail(email);
            response.setMessage(e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            TestUploadResponseData response = new TestUploadResponseData();
            response.setUserEmail(email);
            response.setMessage("Internal server error.");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

}
