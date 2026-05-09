package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.ProviderRequest;
import com.odgiedev.reservvo.dto.response.ProviderResponse;
import com.odgiedev.reservvo.entity.Provider;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import static com.odgiedev.reservvo.util.StringUtils.capitalizeWords;
import static com.odgiedev.reservvo.util.StringUtils.normalizePhone;
import static com.odgiedev.reservvo.util.StringUtils.trimToNull;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderService {

    private final ProviderRepository providerRepository;

    public Provider createDefaultForUser(User user) {
        return providerRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    String firstName = user.getName().trim().split("\\s+")[0].toLowerCase();
                    String uniqueSuffix = user.getId().toString().substring(0, 8);
                    String slug = "%s-%s".formatted(firstName, uniqueSuffix);

                    Provider provider = Provider.builder()
                            .user(user)
                            .businessName("Negócio de %s".formatted(firstName))
                            .slug(slug)
                            .build();

                    Provider saved = providerRepository.save(provider);
                    log.info("Provider default criado | providerId: {} | userId: {}",
                            saved.getId(), user.getId());
                    return saved;
                });
    }

    public ProviderResponse create(ProviderRequest request, User user) {
        if (providerRepository.existsByUserId(user.getId())) {
            throw new BusinessException("Usuário já possui um negócio cadastrado");
        }

        String businessName = capitalizeWords(request.businessName());

        Provider provider = Provider.builder()
                .user(user)
                .businessName(businessName)
                .slug("%s-%s".formatted(user.getName().toLowerCase(), businessName.toLowerCase()))
                .description(trimToNull(request.description()))
                .category(capitalizeWords(request.category()))
                .phone(normalizePhone(request.phone()))
                .build();

        providerRepository.save(provider);
        log.info("Provider criado | providerId: {} | userId: {}", provider.getId(), user.getId());
        return toResponse(provider);
    }

    public ProviderResponse getByUser(User user) {
        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Negócio não encontrado"));
        return toResponse(provider);
    }

    public ProviderResponse getBySlug(String slug) {
        Provider provider = providerRepository.findBySlug(slug)
                .orElseThrow(() -> new BusinessException("Negócio não encontrado"));
        return toResponse(provider);
    }

    public ProviderResponse update(ProviderRequest request, User user) {
        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Negócio não encontrado"));

        if (providerRepository.existsBySlugAndIdNot(request.slug().toLowerCase(), provider.getId())) {
            throw new BusinessException("Slug já está em uso");
        }

        provider.setBusinessName(capitalizeWords(request.businessName()));
        provider.setSlug(request.slug().toLowerCase().trim());
        provider.setDescription(trimToNull(request.description()));
        provider.setCategory(capitalizeWords(request.category()));
        provider.setPhone(normalizePhone(request.phone()));

        providerRepository.save(provider);
        log.info("Provider atualizado | providerId: {}", provider.getId());
        return toResponse(provider);
    }

    public Provider getEntityByUser(User user) {
        return providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new BusinessException("Negócio não encontrado"));
    }

    public Provider getEntityByProviderId(UUID providerId) {
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException("Negócio não encontrado"));
    }

    private ProviderResponse toResponse(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getUser().getId(),
                provider.getBusinessName(),
                provider.getSlug(),
                provider.getDescription(),
                provider.getCategory(),
                provider.getPhone(),
                provider.getCreatedAt()
        );
    }
}