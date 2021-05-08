package com.bigbaldy.poker.service;

import com.bigbaldy.poker.model.BaseModel;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface IService<T extends BaseModel<ID>, ID> {

    Map<ID, T> getMap(List<ID> ids);

    Map<ID, T> getMapFromDb(List<ID> ids);

    Map<ID, T> getMapFromLocalCache(List<ID> ids);

    Optional<T> get(ID id);

    Optional<T> getFromDb(ID id);

    Optional<T> getFromLocalCache(ID id);

    List<T> get(List<ID> ids);

    List<T> getFromDb(List<ID> ids);

    T save(T t);

    T saveToDb(T t);

    List<T> save(List<T> t);

    List<T> saveToDb(List<T> t);

    void remove(ID id);

    void removeFromDb(ID id);

    void remove(List<T> t);

    void removeFromDb(List<T> t);

    List<T> getOnlyFromCache(List<ID> ids);
}
