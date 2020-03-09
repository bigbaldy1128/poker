package com.bigbaldy.poker.model;

import com.bigbaldy.poker.model.type.ThirdPartyUserType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * @author wangjinzhao on 2020/3/9
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "third_party_users")
public class ThirdPartyUser extends AbstractMysqlEntity {
    private Long userId;
    private String thirdPartyUserId;
    private ThirdPartyUserType thirdPartyUserType;

    @Override
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return this.id;
    }
}
