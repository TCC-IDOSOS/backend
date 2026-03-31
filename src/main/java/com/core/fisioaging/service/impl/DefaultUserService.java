package com.core.fisioaging.service.impl;

import com.core.fisioaging.data.UserData;
import com.core.fisioaging.mapper.UserMapper;
import com.core.fisioaging.model.UserModel;
import com.core.fisioaging.repository.UserRepository;
import com.core.fisioaging.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional
public class DefaultUserService implements UserService {

    private final UserRepository userRepository;

    public DefaultUserService(final UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserData create(final UserData userData) {
        final UserModel userModel = UserMapper.toModel(userData);
        userModel.setId(null);
        final UserModel savedUser = userRepository.save(userModel);
        return UserMapper.toData(savedUser);
    }

    @Override
    public UserData update(final Long id, final UserData userData) {
        final UserModel existingUser = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado para o id: " + id));

        UserMapper.updateModel(existingUser, userData);
        final UserModel updatedUser = userRepository.save(existingUser);
        return UserMapper.toData(updatedUser);
    }

    @Override
    public UserData findById(final Long id) {
        final UserModel userModel = userRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Usuário não encontrado para o id: " + id));
        return UserMapper.toData(userModel);
    }

    @Override
    public List<UserData> findAll() {
        return userRepository.findAll()
                .stream()
                .map(UserMapper::toData)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(final Long id) {
        if (!userRepository.existsById(id)) {
            throw new NoSuchElementException("Usuário não encontrado para o id: " + id);
        }
        userRepository.deleteById(id);
    }
}
