package com.qasmartpredict.backend.service.imports;

import com.qasmartpredict.backend.domain.ReleaseEntity;
import com.qasmartpredict.backend.domain.UserStoryEntity;
import com.qasmartpredict.backend.repository.ReleaseRepository;
import com.qasmartpredict.backend.repository.UserStoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraImportService {

    private final UserStoryRepository userStoryRepository;
    private final ReleaseRepository releaseRepository;

    @Transactional
    public int importCsv(MultipartFile file) {

        int importedCount = 0;
        int missingFixVersionCount = 0;
        int unknownReleaseCount = 0;

        try (
                Reader reader = new InputStreamReader(
                        file.getInputStream(),
                        StandardCharsets.UTF_8
                );

                /*
                 * IMPORTANT :
                 * On ne fait PAS setHeader().
                 *
                 * Jira exporte plusieurs colonnes ayant exactement
                 * le même nom "Fix Version/s".
                 *
                 * On doit donc lire la première ligne manuellement
                 * pour conserver les positions des colonnes.
                 */
                CSVParser csvParser = CSVFormat.DEFAULT
                        .builder()
                        .setDelimiter(';')
                        .setTrim(true)
                        .build()
                        .parse(reader)
        ) {

            List<CSVRecord> records = csvParser.getRecords();

            if (records.isEmpty()) {
                log.warn("JIRA IMPORT - Fichier CSV vide.");
                return 0;
            }

            // =========================================================
            // 1. HEADER
            // =========================================================

            CSVRecord headerRecord = records.get(0);

            Map<String, Integer> columnIndexes = new HashMap<>();
            List<Integer> fixVersionIndexes = new ArrayList<>();

            for (int i = 0; i < headerRecord.size(); i++) {

                String header = headerRecord.get(i);

                if (header == null) {
                    continue;
                }

                header = header.trim();

                // Colonnes standards
                columnIndexes.putIfAbsent(header, i);

                // Toutes les colonnes Fix Version/s
                if ("Fix Version/s".equalsIgnoreCase(header)) {
                    fixVersionIndexes.add(i);
                }
            }

            log.info(
                    "JIRA IMPORT - Nombre de colonnes Fix Version détectées : {}",
                    fixVersionIndexes.size()
            );

            log.info(
                    "JIRA IMPORT - Index Fix Version détectés : {}",
                    fixVersionIndexes
            );

            if (fixVersionIndexes.isEmpty()) {
                throw new IllegalStateException(
                        "Aucune colonne Fix Version/s trouvée dans le CSV Jira."
                );
            }

            // =========================================================
            // 2. VERIFICATION COLONNES
            // =========================================================

            requireColumn(columnIndexes, "Issue key");
            requireColumn(columnIndexes, "Issue Type");
            requireColumn(columnIndexes, "Summary");
			requireColumn(        columnIndexes,        "Inward issue link (Development)");

            // =========================================================
            // 3. TICKETS
            // =========================================================

            // index 0 = header
            // les données commencent donc à 1
            for (int rowIndex = 1;
                 rowIndex < records.size();
                 rowIndex++) {

                CSVRecord record = records.get(rowIndex);

                String issueKey = getValue(
                        record,
                        columnIndexes.get("Issue key")
                );

                if (issueKey == null || issueKey.isBlank()) {
                    continue;
                }

                UserStoryEntity userStory =
                        userStoryRepository
                                .findByJiraKey(issueKey)
                                .orElseGet(UserStoryEntity::new);

                if (userStory.getId() == null) {
                    userStory.setId(UUID.randomUUID());
                }

                // =====================================================
                // DONNEES JIRA
                // =====================================================

                userStory.setJiraKey(issueKey);

                userStory.setIssueType(
                        getValue(
                                record,
                                columnIndexes.get("Issue Type")
                        )
                );

                userStory.setSummary(
                        getValue(
                                record,
                                columnIndexes.get("Summary")
                        )
                );

                userStory.setStatus(
                        getValue(
                                record,
                                columnIndexes.get("Status")
                        )
                );
				
				String epicKey = getValue(
        record,
        columnIndexes.get(
                "Inward issue link (Development)"
        )
);

userStory.setEpicKey(epicKey);

                userStory.setSprint(
                        getValue(
                                record,
                                columnIndexes.get("Sprint")
                        )
                );

                userStory.setReporter(
                        getValue(
                                record,
                                columnIndexes.get("Reporter")
                        )
                );

                userStory.setAssignee(
                        getValue(
                                record,
                                columnIndexes.get("Assignee")
                        )
                );

                userStory.setTsTicketId(
                        getValue(
                                record,
                                columnIndexes.get("TS tickets ID")
                        )
                );

                // =====================================================
                // 4. TOUTES LES FIX VERSIONS
                // =====================================================

                Set<String> fixVersions = new LinkedHashSet<>();

                for (Integer fixVersionIndex : fixVersionIndexes) {

                    String version = getValue(
                            record,
                            fixVersionIndex
                    );

                    if (version != null && !version.isBlank()) {
                        fixVersions.add(version.trim());
                    }
                }

                log.debug(
                        "JIRA FIX VERSIONS - {} -> {}",
                        issueKey,
                        fixVersions
                );

                // Debug temporaire important pour nos deux exemples
                if ("FISCDSOL-87833".equals(issueKey)
                        || "FISCDSOL-91306".equals(issueKey)) {

                    log.info(
                            "DEBUG FIX VERSIONS - {} -> {}",
                            issueKey,
                            fixVersions
                    );
                }

                // =====================================================
                // 5. DATA QUALITY
                // =====================================================

                if (fixVersions.isEmpty()) {

                    missingFixVersionCount++;

                    log.warn(
                            "MISSING_FIX_VERSION - Jira {} [{}] " +
                                    "n'a aucune Fix Version.",
                            issueKey,
                            userStory.getIssueType()
                    );
                }

                // =====================================================
                // 6. RECONSTRUCTION DES ASSOCIATIONS
                // =====================================================

                userStory.getReleases().clear();

                for (String version : fixVersions) {

                    ReleaseEntity release =
                            releaseRepository
                                    .findByVersion(version)
                                    .orElse(null);

                    if (release != null) {

                        userStory
                                .getReleases()
                                .add(release);

                        log.debug(
                                "JIRA_RELEASE_LINK - {} -> {}",
                                issueKey,
                                version
                        );

                    } else {

                        unknownReleaseCount++;

                        log.warn(
                                "RELEASE_UNKNOWN - Jira {} référence " +
                                        "la Fix Version '{}' qui n'existe pas " +
                                        "dans SmartPredict.",
                                issueKey,
                                version
                        );
                    }
                }

                userStoryRepository.save(userStory);

                importedCount++;
            }

            // =========================================================
            // 7. SUMMARY
            // =========================================================

            log.info(
                    """
                    
                    ==========================================
                    JIRA IMPORT SUMMARY
                    ==========================================
                    Tickets importés        : {}
                    Sans Fix Version        : {}
                    Références inconnues    : {}
                    ==========================================
                    """,
                    importedCount,
                    missingFixVersionCount,
                    unknownReleaseCount
            );

        } catch (Exception e) {

            log.error(
                    "JIRA IMPORT ERROR",
                    e
            );

            throw new RuntimeException(
                    "Erreur pendant l'import Jira",
                    e
            );
        }

        return importedCount;
    }

    // =============================================================
    // HELPERS
    // =============================================================

    private String getValue(
            CSVRecord record,
            Integer index
    ) {

        if (index == null) {
            return null;
        }

        if (index < 0 || index >= record.size()) {
            return null;
        }

        String value = record.get(index);

        if (value == null) {
            return null;
        }

        return value.trim();
    }

    private void requireColumn(
            Map<String, Integer> columnIndexes,
            String columnName
    ) {

        if (!columnIndexes.containsKey(columnName)) {

            throw new IllegalStateException(
                    "Colonne Jira obligatoire absente : "
                            + columnName
            );
        }
    }
}