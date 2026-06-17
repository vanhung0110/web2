package com.hotelchain.pro.payment.service;

import com.hotelchain.pro.auth.entity.User;
import com.hotelchain.pro.common.exception.ResourceNotFoundException;
import com.hotelchain.pro.common.exception.PaymentException;
import com.hotelchain.pro.payment.dto.BankConfigDto;
import com.hotelchain.pro.payment.dto.UpdateBankConfigRequest;
import com.hotelchain.pro.payment.entity.BankConfig;
import com.hotelchain.pro.property.entity.Property;
import com.hotelchain.pro.property.repository.PropertyRepository;
import com.hotelchain.pro.payment.repository.BankConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BankConfigService {

    private final BankConfigRepository bankConfigRepository;
    private final PropertyRepository propertyRepository;
    private final VietQRService vietQRService;

    public BankConfigDto getBankConfig(UUID propertyId) {
        BankConfig config = bankConfigRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("BankConfig for Property", propertyId.toString()));
        return mapToDto(config);
    }

    @Transactional
    public BankConfigDto updateBankConfig(UUID propertyId, UpdateBankConfigRequest request, User user) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Property", propertyId.toString()));

        BankConfig config = bankConfigRepository.findByPropertyId(propertyId).orElse(null);
        if (config == null) {
            config = new BankConfig();
            config.setProperty(property);
        }

        config.setAccountHolderName(request.getAccountHolderName());
        config.setAccountNumber(request.getAccountNumber());
        config.setBankCode(request.getBankCode());
        config.setBankName(request.getBankCode().getDisplayName());
        config.setBankBin(request.getBankCode().getBin());
        config.setBranch(request.getBranch());
        config.setTemplateDescription(request.getTemplateDescription());
        config.setRequireUtilityPhoto(request.getRequireUtilityPhoto());
        config.setRequireUtilityInput(request.getRequireUtilityInput());
        config.setAutoConfirmEnabled(request.getAutoConfirmEnabled());
        config.setConfirmTimeoutMinutes(request.getConfirmTimeoutMinutes());
        config.setIsActive(true);
        config.setLastUpdated(LocalDateTime.now());
        config.setConfiguredBy(user);

        // Generate static QR
        try {
            String staticQrKey = vietQRService.generateStaticQR(config);
            if (staticQrKey != null) {
                config.setStaticQrImageKey(staticQrKey);
                config.setQrGeneratedAt(LocalDateTime.now());
            }
        } catch (Exception e) {
            // Ignore if VietQR API fails or MinIO fails
        }

        BankConfig saved = bankConfigRepository.save(config);
        return mapToDto(saved);
    }

    public Object getStaticQr(UUID propertyId) {
        BankConfig config = bankConfigRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("BankConfig for Property", propertyId.toString()));

        return Map.of(
                "propertyId", propertyId,
                "staticQrImageKey", config.getStaticQrImageKey() != null ? config.getStaticQrImageKey() : "",
                "qrGeneratedAt", config.getQrGeneratedAt() != null ? config.getQrGeneratedAt() : LocalDateTime.now()
        );
    }

    @Transactional
    public Object regenerateStaticQr(UUID propertyId) {
        BankConfig config = bankConfigRepository.findByPropertyId(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("BankConfig for Property", propertyId.toString()));

        try {
            String staticQrKey = vietQRService.generateStaticQR(config);
            if (staticQrKey != null) {
                config.setStaticQrImageKey(staticQrKey);
                config.setQrGeneratedAt(LocalDateTime.now());
                bankConfigRepository.save(config);
            } else {
                throw new PaymentException("PAYMENT_005", "Không thể sinh QR tĩnh cho ngân hàng");
            }
        } catch (Exception e) {
            throw new PaymentException("PAYMENT_005", "Sinh QR tĩnh thất bại: " + e.getMessage());
        }

        return Map.of(
                "propertyId", propertyId,
                "staticQrImageKey", config.getStaticQrImageKey(),
                "qrGeneratedAt", config.getQrGeneratedAt()
        );
    }

    private BankConfigDto mapToDto(BankConfig config) {
        return BankConfigDto.builder()
                .id(config.getId())
                .propertyId(config.getProperty().getId())
                .accountHolderName(config.getAccountHolderName())
                .accountNumber(config.getAccountNumber())
                .bankCode(config.getBankCode())
                .bankName(config.getBankName())
                .bankBin(config.getBankBin())
                .branch(config.getBranch())
                .isActive(config.getIsActive())
                .templateDescription(config.getTemplateDescription())
                .requireUtilityPhoto(config.getRequireUtilityPhoto())
                .requireUtilityInput(config.getRequireUtilityInput())
                .staticQrImageKey(config.getStaticQrImageKey())
                .autoConfirmEnabled(config.getAutoConfirmEnabled())
                .confirmTimeoutMinutes(config.getConfirmTimeoutMinutes())
                .build();
    }
}
