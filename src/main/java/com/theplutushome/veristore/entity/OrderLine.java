package com.theplutushome.veristore.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
@Entity
@Table(name = "order_lines")
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 64)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false)
    private Integer qty;

    @Column(name = "unit_price_minor", nullable = false)
    private Long unitPriceMinor;

    @Column(name = "total_minor", nullable = false)
    private Long totalMinor;
}
