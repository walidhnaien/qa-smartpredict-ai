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
     * Calcule les indicateurs RCA pour les incidents clients.
     */
    public RcaSummaryDto calculate() {

        /*
         * ==========================================================
         * 1. Récupération des incidents clients depuis Jira
         * ==========================================================
         */

        List<UserStoryEntity> incidents =
                userStoryRepository.findAll()
                        .stream()
                        .filter(this::isBug)
                        .filter(incidentService::isClientIncident)
                        .toList();

        /*
         * ==========================================================
         * 2. Construction de la liste des Jira IDs incidents
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
         * 4. Matching :
         *
         * Jira incident :
         * FISCDSOL-15265
         *
         * RCA :
         * FISCDSOL-15265
         *
         * La normalisation permet d'éviter les problèmes :
         * - espaces
         * - minuscules / majuscules
         * - BOM UTF-8
         * - espaces insécables
         * ==========================================================
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
         * 6. Présence d'actions correctives
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
         * 7. Présence d'actions préventives
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
         *
         * Exemple :
         * 5 RCA / 6 incidents
         * = 83.33 %
         * ==========================================================
         */

        double rcaCoverage =
                percentage(
                        matchedRcas.size(),
                        incidents.size()
                );

        /*
         * ==========================================================
         * 9. Corrective Action Coverage
         *
         * RCA contenant une action corrective
         * /
         * RCA correspondant aux incidents
         * ==========================================================
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
         *
         * RCA Done
         * /
         * RCA correspondant aux incidents
         * ==========================================================
         */

        double closureRate =
                percentage(
                        done,
                        matchedRcas.size()
                );

        /*
         * ==========================================================
         * 12. RCA Score
         *
         * Proposition actuelle :
         *
         * RCA Coverage                 30 %
         * Corrective Action Coverage   25 %
         * Preventive Action Coverage   25 %
         * Closure Rate                 20 %
         *
         * Total = 100 %
         * ==========================================================
         */

        double rcaScore =
                (rcaCoverage * 0.30)
                        + (correctiveActionCoverage * 0.25)
                        + (preventiveActionCoverage * 0.25)
                        + (closureRate * 0.20);

        /*
         * ==========================================================
         * 13. DEBUG CONSOLE
         *
         * Très utile actuellement pour vérifier le matching.
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
                /*
                 * Supprime un éventuel BOM UTF-8
                 */
                .replace("\uFEFF", "")

                /*
                 * Remplace les espaces insécables
                 */
                .replace("\u00A0", " ")

                /*
                 * Supprime espaces début / fin
                 */
                .trim()

                /*
                 * Uniformise la casse
                 */
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
     * Vérifie qu'une action est renseignée.
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