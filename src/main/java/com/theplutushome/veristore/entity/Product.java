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
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String sku;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "short_desc", length = 512)
    private String shortDesc;

    @Column(name = "long_desc", length = 4096)
    private String longDesc;

    @Column(length = 64)
    private String country;

    @Column(length = 128)
    private String category;

    @Column(name = "price_minor", nullable = false)
    private Long priceMinor;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(nullable = false)
    private boolean active = true;
}
