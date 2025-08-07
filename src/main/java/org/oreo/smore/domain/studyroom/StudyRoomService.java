package org.oreo.smore.domain.studyroom;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.oreo.smore.domain.participant.ParticipantRepository;
import org.oreo.smore.domain.studyroom.dto.StudyRoomInfoReadResponse;
import org.oreo.smore.global.common.CursorPage;
import org.oreo.smore.domain.user.User;
import org.oreo.smore.domain.user.UserRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudyRoomService {
    private final StudyRoomRepository roomRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepo;

    public CursorPage<StudyRoomInfoReadResponse> listStudyRooms(
            Long page,
            int limit,
            String search,
            String category,
            String sort,
            boolean hideFullRooms
    ) {
        long cursor = (page != null && page > 1) ? page : Long.MAX_VALUE;;
        Sort sortOrder = buildSortOrder(sort);
        Pageable pageable = PageRequest.of(0, limit + 1, sortOrder);

        List<StudyRoom> rooms = fetchRooms(cursor, search, category, pageable);
        List<StudyRoomInfoReadResponse> dtos = mapAndFilterRooms(rooms, hideFullRooms);
        if (isPopularSort(sort)) {
            applyPopularSort(dtos);
        }
        return CursorPage.of(dtos, limit);
    }

    private Sort buildSortOrder(String sort) {
        String property = isPopularSort(sort) ? "currentParticipants" : "createdAt";
        return Sort.by(Sort.Direction.DESC, property);
    }

    private boolean isPopularSort(String sort) {
        return "popular".equalsIgnoreCase(sort);
    }

    private List<StudyRoom> fetchRooms(
            long cursor,
            String search,
            String category,
            Pageable pageable
    ) {
        Specification<StudyRoom> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            predicates.add(cb.lessThan(root.get("roomId"), cursor));
            addSearchPredicate(cb, root, predicates, search);
            addCategoryPredicate(cb, root, predicates, category);
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return roomRepository.findAll(spec, pageable).getContent();
    }

    private void addSearchPredicate(
            CriteriaBuilder cb,
            Root<StudyRoom> root,
            List<Predicate> preds,
            String search
    ) {
        if (search != null && !search.isBlank()) {
            preds.add(cb.like(
                    cb.lower(root.get("title")),
                    "%" + search.toLowerCase() + "%"
            ));
        }
    }

    private void addCategoryPredicate(
            CriteriaBuilder cb,
            Root<StudyRoom> root,
            List<Predicate> preds,
            String category
    ) {
        if (category != null && !category.isBlank()) {
            try {
                StudyRoomCategory catEnum = StudyRoomCategory.valueOf(category.toUpperCase());
                preds.add(cb.equal(root.get("category"), catEnum));
            } catch (IllegalArgumentException e) {
                preds.add(cb.disjunction());
            }
        }
    }

    private List<StudyRoomInfoReadResponse> mapAndFilterRooms(
            List<StudyRoom> rooms,
            boolean hideFullRooms
    ) {
        return rooms.stream()
                .map(this::toDto)
                .filter(dto -> !hideFullRooms || dto.getCurrentParticipants() < dto.getMaxParticipants())
                .collect(Collectors.toList());
    }

    private StudyRoomInfoReadResponse toDto(StudyRoom room) {
        long count = participantRepository.countByRoomIdAndLeftAtIsNull(room.getRoomId());
        User creator = userRepo.findById(room.getUserId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Creator not found: " + room.getUserId()
                ));
        return StudyRoomInfoReadResponse.of(room, count, creator.getNickname());
    }

    private void applyPopularSort(List<StudyRoomInfoReadResponse> dtos) {
        dtos.sort(
                Comparator.comparingLong(StudyRoomInfoReadResponse::getCurrentParticipants).reversed()
        );
    }
}
