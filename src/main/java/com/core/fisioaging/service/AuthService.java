package com.core.fisioaging.service;

import com.core.fisioaging.data.LoginData;
import com.core.fisioaging.data.TokenData;

public interface AuthService {

    TokenData login(LoginData loginData);
}
