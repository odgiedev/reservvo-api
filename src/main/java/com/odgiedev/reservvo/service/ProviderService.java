package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.ProviderRequest;
import com.odgiedev.reservvo.dto.response.ProviderResponse;
import com.odgiedev.reservvo.entity.Provider;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;

    public ProviderResponse create(ProviderRequest request, User user) {
        if (providerRepository.existsByUserId(user.getId())) {
            throw new BusinessException("Usuário já possui um negócio cadastrado");
        }

        Provider provider = Provider.builder()
                .user(user)
                .businessName(request.businessName())
                .description(request.description())
                .category(request.category())
                .build();

        providerRepository.save(provider);
        return toResponse(provider);
    }

    public ProviderResponse getByUser(User user) {
        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Negócio não encontrado"));
        return toResponse(provider);
    }

    public ProviderResponse update(ProviderRequest request, User user) {
        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Negócio não encontrado"));

        provider.setBusinessName(request.businessName());
        provider.setDescription(request.description());
        provider.setCategory(request.category());

        providerRepository.save(provider);
        return toResponse(provider);
    }

    public Provider getEntityByUser(User user) {
        return providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Negócio não encontrado"));
    }

    private ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getUser().getId(),
                provider.getBusinessName(),
                provider.getDescription(),
                provider.getCategory(),
                provider.getCreatedAt()
        );
    }
}