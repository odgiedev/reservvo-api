package com.odgiedev.reservvo.service;

import com.odgiedev.reservvo.dto.request.AvailabilityRuleRequest;
import com.odgiedev.reservvo.dto.request.ResourceRequest;
import com.odgiedev.reservvo.dto.response.AvailabilityRuleResponse;
import com.odgiedev.reservvo.dto.response.ResourceResponse;
import com.odgiedev.reservvo.entity.AvailabilityRule;
import com.odgiedev.reservvo.entity.Provider;
import com.odgiedev.reservvo.entity.Resource;
import com.odgiedev.reservvo.entity.User;
import com.odgiedev.reservvo.exception.BusinessException;
import com.odgiedev.reservvo.repository.AvailabilityRuleRepository;
import com.odgiedev.reservvo.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final ProviderService providerService;

    public ResourceResponse create(ResourceRequest request, User user) {
        Provider provider = providerService.getEntityByUser(user);

        Resource resource = Resource.builder()
                .provider(provider)
                .name(request.name())
                .description(request.description())
                .slotDurationMin(request.slotDurationMin())
                .active(true)
                .build();

        resourceRepository.save(resource);
        return toResponse(resource);
    }

    public List<ResourceResponse> listByProvider(User user) {
        Provider provider = providerService.getEntityByUser(user);
        return resourceRepository.findByProviderIdAndActiveTrue(provider.getId())
                .stream().map(this::toResponse).toList();
    }

    public ResourceResponse update(UUID resourceId, ResourceRequest request, User user) {
        Resource resource = getResourceOwnedByUser(resourceId, user);
        resource.setName(request.name());
        resource.setDescription(request.description());
        resource.setSlotDurationMin(request.slotDurationMin());
        resourceRepository.save(resource);
        return toResponse(resource);
    }

    public ResourceResponse deactivate(UUID resourceId, User user) {
        Resource resource = getResourceOwnedByUser(resourceId, user);
        resource.setActive(false);
        resourceRepository.save(resource);

        return toResponse(resource);
    }

    @Transactional
    public List<AvailabilityRuleResponse> setAvailabilityRules(
            UUID resourceId, List<AvailabilityRuleRequest> rules, User user) {

        Resource resource = getResourceOwnedByUser(resourceId, user);

        // valida dias duplicados
        List<Integer> days = rules.stream().map(AvailabilityRuleRequest::dayOfWeek).toList();

        if (days.size() != days.stream().distinct().count()) {
            throw new BusinessException("Não é permitido cadastrar o mesmo dia da semana mais de uma vez");
        }

        // valida se startTime é antes do endTime
        rules.forEach(r -> {
            if (!r.startTime().isBefore(r.endTime())) {
                throw new BusinessException("Horário de início deve ser anterior ao horário de fim");
            }
        });

        availabilityRuleRepository.deleteByResourceId(resourceId);

        List<AvailabilityRule> newRules = rules.stream().map(r -> AvailabilityRule.builder()
                .resource(resource)
                .dayOfWeek(r.dayOfWeek())
                .startTime(r.startTime())
                .endTime(r.endTime())
                .build()).toList();

        availabilityRuleRepository.saveAll(newRules);
        return newRules.stream().map(this::toRuleResponse).toList();
    }

    public List<AvailabilityRuleResponse> getAvailabilityRules(UUID resourceId) {
        return availabilityRuleRepository.findByResourceId(resourceId)
                .stream().map(this::toRuleResponse).toList();
    }

    private Resource getResourceOwnedByUser(UUID resourceId, User user) {
        Provider provider = providerService.getEntityByUser(user);
        Resource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException("Recurso não encontrado"));
        if (!resource.getProvider().getId().equals(provider.getId())) {
            throw new BusinessException("Você não tem permissão sobre esse recurso");
        }
        return resource;
    }

    private ResourceResponse toResponse(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getProvider().getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getSlotDurationMin(),
                resource.getActive()
        );
    }

    private AvailabilityRuleResponse toRuleResponse(AvailabilityRule rule) {
        return new AvailabilityRuleResponse(
                rule.getId(),
                rule.getResource().getId(),
                rule.getDayOfWeek(),
                rule.getStartTime(),
                rule.getEndTime()
        );
    }
}