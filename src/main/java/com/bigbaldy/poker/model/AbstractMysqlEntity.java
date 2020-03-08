package com.bigbaldy.poker.model;

import javax.persistence.MappedSuperclass;
import java.time.Instant;

@MappedSuperclass
public abstract class AbstractMysqlEntity implements BaseModel<Long> {

    protected Long id;

    protected Instant createdAt;

    protected Instant updatedAt;

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
