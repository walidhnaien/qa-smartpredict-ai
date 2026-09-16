package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.UserStoryEntity;
import com.qasmartpredict.backend.dto.ClientIncidentDto;
import com.qasmartpredict.backend.dto.IncidentSummaryDto;
import com.qasmartpredict.backend.repository.UserStoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class IncidentService {

    private final UserStoryRepository userStoryRepository;

    private final List<String> clients;

    /*
     * Exemple :
     * TS 4144919
     * TS1313214
     * TS-123456
     */
    private static final Pattern TS_PATTERN =
            Pattern.compile(
                    "\\bTS[\\s\\-:]?\\d+\\b",
                    Pattern.CASE_INSENSITIVE
            );

    /*
     * Exemple :
     * Ticket 2116278
     * Ticket [2116278]
     * Ticket: 2116278
     */
    private static final Pattern TICKET_PATTERN =
            Pattern.compile(
                    "\\bTICKET[\\s\\-:\\[]*\\d+",
                    Pattern.CASE_INSENSITIVE
            );

    public IncidentService(
            UserStoryRepository userStoryRepository,
            @Value("${quality.incident.clients:Cargill,Bunge,Barclays}")
            String clientsConfig) {

        this.userStoryRepository = userStoryRepository;

        this.clients = Arrays.stream(clientsConfig.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    public IncidentSummaryDto calculate() {

        /*
         * 1. Tous les bugs Jira
         */
        List<UserStoryEntity> bugs =
                userStoryRepository.findAll()
                        .stream()
                        .filter(this::isBug)
                        .toList();

        /*
         * 2. Incidents clients :
         * uniquement les bugs avec client identifié
         */
        List<UserStoryEntity> incidents =
                bugs.stream()
                        .filter(this::isClientIncident)
                        .toList();

        /*
         * 3. Bugs internes :
         * bugs qui ne sont pas des incidents clients
         */
        List<UserStoryEntity> internalBugs =
                bugs.stream()
                        .filter(bug -> !isClientIncident(bug))
                        .toList();

        /*
         * 4. Nombre de bugs clients critiques
         */
        long criticalClientBugCount =
                incidents.stream()
                        .filter(this::isCritical)
                        .count();

        /*
         * 5. Taux d'incidents clients parmi les bugs
         */
        double incidentRate =
                percentage(
                        incidents.size(),
                        bugs.size()
                );

        /*
         * 6. Incident Score
         *
         * Plus la proportion d'incidents clients est élevée,
         * plus le score baisse.
         */
        double incidentScore =
                Math.max(
                        0.0,
                        100.0 - incidentRate
                );

        /*
         * 7. Feedback Score
         *
         * Règle métier :
         * - aucun bug critique client -> 100
         * - au moins un bug critique client -> 0
         */
        double feedbackScore =
                criticalClientBugCount == 0
                        ? 100.0
                        : 0.0;

        /*
         * 8. Regroupement des incidents par client
         */
        Map<String, List<UserStoryEntity>> incidentsByClient =
                new HashMap<>();

        for (UserStoryEntity incident : incidents) {

            String client =
                    detectClient(
                            incident.getSummary()
                    );

            /*
             * Normalement client ne peut pas être null ici,
             * car isClientIncident() vérifie déjà sa présence.
             */
            if (client != null) {

                incidentsByClient
                        .computeIfAbsent(
                                client,
                                key -> new ArrayList<>()
                        )
                        .add(incident);
            }
        }

        /*
         * 9. Statistiques par client
         */
        List<ClientIncidentDto> clientStats =
                incidentsByClient.entrySet()
                        .stream()
                        .map(entry ->
                                new ClientIncidentDto(
                                        entry.getKey(),
                                        entry.getValue().size(),
                                        entry.getValue()
                                                .stream()
                                                .filter(this::isCritical)
                                                .count()
                                )
                        )
                        .sorted(
                                Comparator
                                        .comparingLong(
                                                ClientIncidentDto::incidentCount
                                        )
                                        .reversed()
                        )
                        .toList();

        /*
         * 10. Client le plus impacté
         */
        String mostImpactedClient =
                clientStats.isEmpty()
                        ? "N/A"
                        : clientStats.get(0).client();

        /*
         * Debug utile dans la console
         */
        System.out.println(
                "Total bugs = " + bugs.size()
        );

        System.out.println(
                "Client incidents = " + incidents.size()
        );

        System.out.println(
                "Internal bugs = " + internalBugs.size()
        );

        System.out.println(
                "Critical client bugs = "
                        + criticalClientBugCount
        );

        /*
         * 11. Construction du DTO
         */
        return new IncidentSummaryDto(
                bugs.size(),                 // totalBugs = 34
        internalBugs.size(),         // internalBugs = 28
        incidents.size(),            // totalIncidents = 6
        criticalClientBugCount,      // 0
        round(incidentRate),         // 17.65
        round(incidentScore),        // 82.35
        round(feedbackScore),        // 100
        mostImpactedClient,          // Cargill
        clientStats
        );
    }

    /**
     * Un incident client est un bug
     * dont le client est explicitement identifié.
     */
    public boolean isClientIncident(
            UserStoryEntity issue) {

        if (issue == null) {
            return false;
        }

        String summary =
                Optional
                        .ofNullable(issue.getSummary())
                        .orElse("");

        return detectClient(summary) != null;
    }

    /**
     * Vérifie si le bug possède un ticket support.
     *
     * Cette information n'est plus utilisée
     * pour définir un incident client.
     */
    public boolean hasSupportTicket(
            UserStoryEntity issue) {

        if (issue == null) {
            return false;
        }

        String summary =
                Optional
                        .ofNullable(issue.getSummary())
                        .orElse("");

        String tsTicket =
                Optional
                        .ofNullable(issue.getTsTicketId())
                        .map(String::valueOf)
                        .orElse("");

        return !tsTicket.isBlank()
                || TS_PATTERN.matcher(summary).find()
                || TICKET_PATTERN.matcher(summary).find();
    }

    /**
     * Vérifie si l'issue est un bug Jira.
     */
    private boolean isBug(
            UserStoryEntity issue) {

        return issue != null
                && "BUG".equalsIgnoreCase(
                        Optional
                                .ofNullable(
                                        issue.getIssueType()
                                )
                                .orElse("")
                );
    }

    /**
     * Bug critique client.
     *
     * Blocker ou Critical sont considérés critiques.
     */
    private boolean isCritical(
            UserStoryEntity issue) {

        if (issue == null) {
            return false;
        }

        String priority =
                Optional
                        .ofNullable(issue.getPriority())
                        .orElse("")
                        .trim();

        return priority.equalsIgnoreCase("BLOCKER")
                || priority.equalsIgnoreCase("CRITICAL");
    }

    /**
     * Recherche le client dans le Summary Jira.
     */
    private String detectClient(
            String summary) {

        if (summary == null
                || summary.isBlank()) {

            return null;
        }

        String normalizedSummary =
                summary.toLowerCase(
                        Locale.ROOT
                );

        for (String client : clients) {

            if (normalizedSummary.contains(
                    client.toLowerCase(
                            Locale.ROOT
                    )
            )) {

                return client;
            }
        }

        return null;
    }

    /**
     * Calcul d'un pourcentage.
     */
    private double percentage(
            long value,
            long total) {

        if (total == 0) {
            return 0.0;
        }

        return ((double) value / total)
                * 100.0;
    }

    /**
     * Arrondi à deux décimales.
     */
    private double round(
            double value) {

        return Math.round(
                value * 100.0
        ) / 100.0;
    }
}