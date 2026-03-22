package com.core.fisioaging.service.impl;

import com.core.fisioaging.dao.UserDAO;
import com.core.fisioaging.model.UserModel;
import com.core.fisioaging.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class DefaultUserService implements UserService {

    private final UserDAO userDAO;

    public DefaultUserService(final UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public UserModel create(final UserModel user) {
        return userDAO.save(user);
    }

    @Override
    public UserModel update(final Long id, final UserModel user) {
        final UserModel existingUser = userDAO.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado para o id: " + id));

        existingUser.setName(user.getName());
        existingUser.setCpf(user.getCpf());
        existingUser.setGenre(user.getGenre());
        existingUser.setBirthDate(user.getBirthDate());
        existingUser.setEmail(user.getEmail());
        existingUser.setPassword(user.getPassword());
        existingUser.setAddress(user.getAddress());
        existingUser.setProfile(user.getProfile());

        return userDAO.save(existingUser);
    }

    @Override
    public UserModel findById(final Long id) {
        return userDAO.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado para o id: " + id));
    }

    @Override
    public List<UserModel> findAll() {
        return userDAO.findAll();
    }

    @Override
    public void delete(final Long id) {
        if (!userDAO.existsById(id)) {
            throw new NoSuchElementException("Usuário não encontrado para o id: " + id);
        }

        userDAO.deleteById(id);
    }
}
