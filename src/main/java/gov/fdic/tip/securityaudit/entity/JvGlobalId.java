package gov.fdic.tip.securityaudit.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "jv_global_id")
@Getter
public class JvGlobalId {
    @Id
    @Column(name = "global_id_pk")
    private Long globalIdPk;

    @Column(name = "type_name")
    private String typeName;

    @Column(name = "local_id")
    private String localId;
}