package com.hotelchain.pro.utility.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdatePricesRequest {
    private BigDecimal waterPricePerUnit;
    private BigDecimal electricTier1Price;
    private BigDecimal electricTier2Price;
    private BigDecimal electricTier3Price;
    private BigDecimal electricTier4Price;
    private BigDecimal electricTier5Price;
    private BigDecimal electricTier6Price;
    private Boolean useFixedElectricPrice;
    private BigDecimal fixedElectricPrice;
}
