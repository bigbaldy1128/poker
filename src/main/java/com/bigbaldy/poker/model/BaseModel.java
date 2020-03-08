package com.bigbaldy.poker.model;

import java.time.Instant;

public interface BaseModel<ID> {

    ID getId();

    Instant getCreatedAt();

    void setCreatedAt(Instant instant);

    Instant getUpdatedAt();

    void setUpdatedAt(Instant instant);

}
