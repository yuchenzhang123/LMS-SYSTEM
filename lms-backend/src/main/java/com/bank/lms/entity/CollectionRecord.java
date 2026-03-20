package com.bank.lms.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 催收记录实体
 */
@Data
@Entity
@Table(name = "collection_record")
@EqualsAndHashCode(callSuper = true)
public class CollectionRecord extends BaseEntity {

    @Id
    @Column(name = "record_id", length = 32)
    private String recordId;

    @Column(name = "loan_account", length = 32, nullable = false)
    private String loanAccount;

    @Column(name = "customer_id", length = 32, nullable = false)
    private String customerId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "method", length = 20, nullable = false)
    private String method;

    @Column(name = "method_text", length = 50)
    private String methodText;

    @Column(name = "result", length = 500)
    private String result;

    @Column(name = "operator_id", length = 32)
    private String operatorId;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "operate_time")
    private LocalDateTime operateTime;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "material_type", length = 20)
    private String materialType;

    @Column(name = "material_name", length = 200)
    private String materialName;

    @Column(name = "material_url", length = 500)
    private String materialUrl;

    @Column(name = "extra_data", columnDefinition = "TEXT")
    private String extraData;
}
