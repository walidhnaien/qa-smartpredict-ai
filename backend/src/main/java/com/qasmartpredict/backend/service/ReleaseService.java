package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.ReleaseEntity;
import com.qasmartpredict.backend.repository.ReleaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final ReleaseRepository releaseRepository;

    public List<ReleaseEntity> findAll() {
        return releaseRepository.findAll();
    }
}