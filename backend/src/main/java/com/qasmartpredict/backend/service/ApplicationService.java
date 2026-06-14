package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.ApplicationEntity;
import com.qasmartpredict.backend.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;

    public List<ApplicationEntity> findAll() {
        return applicationRepository.findAll();
    }
}