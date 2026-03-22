package com.core.fisioaging.dao.impl;

import com.core.fisioaging.dao.UserDAO;
import com.core.fisioaging.model.UserModel;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class DefaultInMemoryUserDAO implements UserDAO {

    private final ConcurrentHashMap<Long, UserModel> database = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public UserModel save(final UserModel user) {
        if (user.getId() == null) {
            user.setId(idGenerator.getAndIncrement());
        }

        database.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<UserModel> findById(final Long id) {
        return Optional.ofNullable(database.get(id));
    }

    @Override
    public List<UserModel> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public void deleteById(final Long id) {
        database.remove(id);
    }

    @Override
    public boolean existsById(final Long id) {
        return database.containsKey(id);
    }
}
