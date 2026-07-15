package com.repoary.backend.repository.service;

import com.repoary.backend.repository.domain.ConnectedRepository;
import com.repoary.backend.repository.dto.ConnectRepositoryRequest;
import com.repoary.backend.repository.dto.ConnectedRepositoryResponse;
import com.repoary.backend.repository.repository.ConnectedRepositoryRepository;
import com.repoary.backend.user.domain.User;
import com.repoary.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConnectedRepositoryService {

    private final ConnectedRepositoryRepository connectedRepositoryRepository;
    private final UserRepository userRepository;

    public ConnectedRepositoryService(
            ConnectedRepositoryRepository connectedRepositoryRepository,
            UserRepository userRepository
    ) {
        this.connectedRepositoryRepository = connectedRepositoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ConnectedRepositoryResponse connect(Long userId, ConnectRepositoryRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ConnectedRepository connectedRepository = connectedRepositoryRepository
                .findByUserAndGithubRepositoryId(user, request.githubRepositoryId())
                .orElseGet(() -> connectedRepositoryRepository.save(
                        new ConnectedRepository(
                                user,
                                request.githubRepositoryId(),
                                request.name(),
                                request.fullName(),
                                request.htmlUrl(),
                                request.privateRepository(),
                                request.defaultBranch()
                        )
                ));

        return ConnectedRepositoryResponse.from(connectedRepository);
    }

    @Transactional(readOnly = true)
    public List<ConnectedRepositoryResponse> getConnectedRepositories(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return connectedRepositoryRepository.findAllByUserOrderByConnectedAtDesc(user)
                .stream()
                .map(ConnectedRepositoryResponse::from)
                .toList();
    }

    @Transactional
    public void disconnect(Long userId, Long githubRepositoryId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        connectedRepositoryRepository
                .findByUserAndGithubRepositoryId(user, githubRepositoryId)
                .ifPresent(connectedRepositoryRepository::delete);
    }
}