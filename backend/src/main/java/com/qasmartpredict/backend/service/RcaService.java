package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.RcaEntity;
import com.qasmartpredict.backend.domain.UserStoryEntity;
import com.qasmartpredict.backend.dto.RcaSummaryDto;
import com.qasmartpredict.backend.repository.RcaRepository;
import com.qasmartpredict.backend.repository.UserStoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RcaService {

    private final RcaRepository rcaRepository;
    private final UserStoryRepository userStoryRepository;
    private final IncidentService incidentService;

    public RcaService(
            RcaRepository rcaRepository,
            UserStoryRepository userStoryRepository,
            IncidentService incidentService) {

        this.rcaRepository = rcaRepository;
        this.userStoryRepository = userStoryRepository;
        this.incidentService = incidentService;
    }

    /**
     * ==========================================================
     * CALCUL RCA GLOBAL
     * ==========================================================
     *
     * Conserve le fonctionnement historique.
     *
     * Tous les tickets Jira sont pris en compte.
     */
    public RcaSummaryDto calculate() {

        List<UserStoryEntity> issues =
                userStoryRepository.findAll();

        return calculateFromIssues(issues);
    }

    /**
     * ==========================================================
     * CALCUL RCA PAR RELEASE
     * ==========================================================
     *
     * Seuls les tickets appartenant à la Release sélectionnée
     * sont pris en compte.
     */
    public RcaSummaryDto calculate(UUID releaseId) {

        if (releaseId == null) {
            throw new IllegalArgumentException(
                    "releaseId ne peut pas être null"
            );
        }

        List<UserStoryEntity> issues =
                userStoryRepository
                        .findDistinctByReleases_Id(releaseId);

        return calculateFromIssues(issues);
    }

    /**
     * ==========================================================
     * MOTEUR COMMUN DU CALCUL RCA
     * ==========================================================
     *
     * Cette méthode peut recevoir :
     *
     * - tous les tickets Jira
     * - ou uniquement les tickets d'une Release
     */
    private RcaSummaryDto calculateFromIssues(
            List<UserStoryEntity> issues) {

        /*
         * ==========================================================
         * 1. Récupération des incidents clients
         * ==========================================================
         *
         * On garde uniquement :
         *
         * - Issue Type = Bug
         * - Bug identifié comme incident client
         *
         * Exemple :
         * Summary contenant "Barclays"
         */
        List<UserStoryEntity> incidents =
                issues.stream()
                        .filter(this::isBug)
                        .filter(incidentService::isClientIncident)
                        .toList();

        /*
         * ==========================================================
         * 2. Construction des Jira IDs des incidents
         * ==========================================================
         */
        Set<String> incidentJiraKeys =
                incidents.stream()
                        .map(UserStoryEntity::getJiraKey)
                        .filter(Objects::nonNull)
                        .map(this::normalizeJiraId)
                        .filter(value -> !value.isBlank())
                        .collect(Collectors.toSet());

        /*
         * ==========================================================
         * 3. Lecture des RCA
         * ==========================================================
         */
        List<RcaEntity> allRcas =
                rcaRepository.findAll();

        /*
         * ==========================================================
         * 4. Matching Jira incident <-> RCA
         * ==========================================================
         *
         * Exemple :
         *
         * Jira :
         * FISCDSOL-15265
         *
         * RCA :
         * FISCDSOL-15265
         *
         * Le Jira ID est normalisé afin d'éviter les problèmes :
         *
         * - espaces
         * - minuscules / majuscules
         * - BOM UTF-8
         * - espaces insécables
         */
        List<RcaEntity> matchedRcas =
                allRcas.stream()
                        .filter(Objects::nonNull)
                        .filter(rca -> rca.getJiraId() != null)
                        .filter(rca ->
                                incidentJiraKeys.contains(
                                        normalizeJiraId(
                                                rca.getJiraId()
                                        )
                                )
                        )
                        .toList();

        /*
         * ==========================================================
         * 5. Statuts RCA
         * ==========================================================
         */
        long done =
                matchedRcas.stream()
                        .filter(rca ->
                                statusEquals(
                                        rca.getStatus(),
                                        "DONE"
                                )
                        )
                        .count();

        long inProgress =
                matchedRcas.stream()
                        .filter(rca ->
                                statusEquals(
                                        rca.getStatus(),
                                        "IN PROGRESS"
                                )
                        )
                        .count();

        long toDo =
                matchedRcas.stream()
                        .filter(rca ->
                                statusEquals(
                                        rca.getStatus(),
                                        "TO DO"
                                )
                        )
                        .count();

        /*
         * ==========================================================
         * 6. Actions correctives
         * ==========================================================
         */
        long correctiveActions =
                matchedRcas.stream()
                        .filter(rca ->
                                hasValue(
                                        rca.getCorrectiveAction()
                                )
                        )
                        .count();

        /*
         * ==========================================================
         * 7. Actions préventives
         * ==========================================================
         */
        long preventiveActions =
                matchedRcas.stream()
                        .filter(rca ->
                                hasValue(
                                        rca.getPreventiveAction()
                                )
                        )
                        .count();

        /*
         * ==========================================================
         * 8. RCA Coverage
         * ==========================================================
         *
         * RCA trouvés
         * ---------------- x 100
         * Incidents clients
         */
        double rcaCoverage =
                percentage(
                        matchedRcas.size(),
                        incidents.size()
                );

        /*
         * ==========================================================
         * 9. Corrective Action Coverage
         * ==========================================================
         *
         * RCA avec action corrective
         * --------------------------- x 100
         * RCA trouvés
         */
        double correctiveActionCoverage =
                percentage(
                        correctiveActions,
                        matchedRcas.size()
                );

        /*
         * ==========================================================
         * 10. Preventive Action Coverage
         * ==========================================================
         */
        double preventiveActionCoverage =
                percentage(
                        preventiveActions,
                        matchedRcas.size()
                );

        /*
         * ==========================================================
         * 11. Closure Rate
         * ==========================================================
         *
         * RCA DONE
         * --------- x 100
         * RCA trouvés
         */
        double closureRate =
                percentage(
                        done,
                        matchedRcas.size()
                );

        /*
         * ==========================================================
         * 12. RCA SCORE
         * ==========================================================
         *
         * RCA Coverage                = 30 %
         * Corrective Action Coverage  = 25 %
         * Preventive Action Coverage  = 25 %
         * Closure Rate                = 20 %
         *
         * TOTAL = 100 %
         */
        double rcaScore =
                (rcaCoverage * 0.30)
                        + (correctiveActionCoverage * 0.25)
                        + (preventiveActionCoverage * 0.25)
                        + (closureRate * 0.20);

        /*
         * ==========================================================
         * 13. DEBUG
         * ==========================================================
         */
        System.out.println(
                "=========================================="
        );

        System.out.println(
                "RCA ANALYSIS"
        );

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "Issues analysed = "
                        + issues.size()
        );

        System.out.println(
                "Total client incidents = "
                        + incidents.size()
        );

        System.out.println(
                "Incident Jira IDs = "
                        + incidentJiraKeys
        );

        System.out.println(
                "Total RCA in database = "
                        + allRcas.size()
        );

        System.out.println(
                "RCA Jira IDs = "
                        + allRcas.stream()
                        .map(RcaEntity::getJiraId)
                        .filter(Objects::nonNull)
                        .map(this::normalizeJiraId)
                        .toList()
        );

        System.out.println(
                "Matched RCA = "
                        + matchedRcas.size()
        );

        System.out.println(
                "Matched Jira IDs = "
                        + matchedRcas.stream()
                        .map(RcaEntity::getJiraId)
                        .filter(Objects::nonNull)
                        .map(this::normalizeJiraId)
                        .toList()
        );

        System.out.println(
                "Done = " + done
        );

        System.out.println(
                "In Progress = " + inProgress
        );

        System.out.println(
                "To Do = " + toDo
        );

        System.out.println(
                "Corrective Actions = "
                        + correctiveActions
        );

        System.out.println(
                "Preventive Actions = "
                        + preventiveActions
        );

        System.out.println(
                "RCA Coverage = "
                        + round(rcaCoverage)
                        + "%"
        );

        System.out.println(
                "Corrective Action Coverage = "
                        + round(correctiveActionCoverage)
                        + "%"
        );

        System.out.println(
                "Preventive Action Coverage = "
                        + round(preventiveActionCoverage)
                        + "%"
        );

        System.out.println(
                "Closure Rate = "
                        + round(closureRate)
                        + "%"
        );

        System.out.println(
                "RCA Score = "
                        + round(rcaScore)
        );

        System.out.println(
                "=========================================="
        );

        /*
         * ==========================================================
         * 14. Construction du résultat API
         * ==========================================================
         */
        return new RcaSummaryDto(
                incidents.size(),
                matchedRcas.size(),
                done,
                inProgress,
                toDo,
                round(rcaCoverage),
                round(correctiveActionCoverage),
                round(preventiveActionCoverage),
                round(closureRate),
                round(rcaScore)
        );
    }

    /**
     * Vérifie que l'issue Jira est un Bug.
     */
    private boolean isBug(
            UserStoryEntity issue) {

        if (issue == null) {
            return false;
        }

        String issueType =
                Objects.toString(
                        issue.getIssueType(),
                        ""
                ).trim();

        return issueType.equalsIgnoreCase("BUG");
    }

    /**
     * Normalisation des Jira IDs.
     *
     * Exemple :
     *
     * " fiscdsol-15265 "
     *
     * devient :
     *
     * "FISCDSOL-15265"
     */
    private String normalizeJiraId(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\uFEFF", "")
                .replace("\u00A0", " ")
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    /**
     * Comparaison robuste des statuts RCA.
     */
    private boolean statusEquals(
            String actual,
            String expected) {

        if (actual == null
                || expected == null) {

            return false;
        }

        return actual
                .replace("\uFEFF", "")
                .replace("\u00A0", " ")
                .trim()
                .equalsIgnoreCase(
                        expected.trim()
                );
    }

    /**
     * Vérifie qu'une valeur est renseignée.
     */
    private boolean hasValue(
            String value) {

        return value != null
                && !value.trim().isBlank();
    }

    /**
     * Calcul sécurisé d'un pourcentage.
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