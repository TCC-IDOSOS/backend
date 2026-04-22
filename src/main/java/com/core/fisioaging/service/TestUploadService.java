package com.core.fisioaging.service;

import com.core.fisioaging.data.TestUploadData;
import com.core.fisioaging.data.TestUploadResponseData;

public interface TestUploadService {

    TestUploadResponseData uploadTest(String email, TestUploadData data);

}
