package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.RequirementEntity;
import com.qasmartpredict.backend.repository.RequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RequirementService {

    private final RequirementRepository requirementRepository;

    public List<RequirementEntity> findAll() {
        return requirementRepository.findAll();
    }
}