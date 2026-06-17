package com.hotelchain.pro.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Danh sách ngân hàng Việt Nam hỗ trợ VietQR.
 * BIN code dùng để tạo QR theo chuẩn Napas/VietQR.
 */
@Getter
@RequiredArgsConstructor
public enum BankCode {
    VCB("970436", "Vietcombank"),
    BIDV("970418", "BIDV"),
    VTB("970415", "Vietinbank"),
    AGR("970405", "Agribank"),
    MB("970422", "MB Bank"),
    TCB("970407", "Techcombank"),
    ACB("970416", "ACB"),
    VPB("970432", "VPBank"),
    TPB("970423", "TPBank"),
    STB("970403", "Sacombank"),
    EIB("970431", "Eximbank"),
    HDB("970437", "HDBank"),
    OCB("970448", "OCB"),
    SHB("970443", "SHB"),
    BAB("970409", "BAC A BANK"),
    MSB("970426", "MSB"),
    NAB("970428", "Nam A Bank"),
    PGB("970430", "PG Bank"),
    SEAB("970440", "SeABank"),
    VIB("970441", "VIB"),
    LPB("970449", "LienVietPostBank"),
    KLB("970452", "Kienlong Bank"),
    BVB("970454", "BaoViet Bank"),
    VIETBANK("970433", "VietBank"),
    CAKE("546034", "CAKE by VPBank"),
    TIMO("963388", "Timo"),
    MOMO("MOMO", "MoMo"),
    ZALOPAY("ZALO", "ZaloPay");

    private final String bin;
    private final String displayName;
}
