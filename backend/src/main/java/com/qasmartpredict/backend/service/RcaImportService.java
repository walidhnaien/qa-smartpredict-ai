package com.qasmartpredict.backend.service;

import com.qasmartpredict.backend.domain.RcaEntity;
import com.qasmartpredict.backend.repository.RcaRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class RcaImportService {

    private final RcaRepository rcaRepository;

    public RcaImportService(RcaRepository rcaRepository) {
        this.rcaRepository = rcaRepository;
    }

    /**
     * Importe le fichier RCA comme un snapshot complet.
     *
     * Principe :
     * 1. Lecture et validation du CSV
     * 2. Normalisation des Jira ID
     * 3. Gestion des doublons : dernière ligne gagnante
     * 4. Suppression de l'ancien snapshot RCA
     * 5. Enregistrement du nouveau snapshot
     *
     * @return nombre de Jira ID uniques réellement enregistrés
     */
    @Transactional
    public int importCsv(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Le fichier RCA est vide."
            );
        }

        /*
         * LinkedHashMap :
         * - clé   = Jira ID normalisé
         * - valeur = RCA
         *
         * Si le même Jira ID apparaît plusieurs fois,
         * la dernière ligne du CSV remplace la précédente.
         */
        Map<String, RcaEntity> uniqueRcas =
                new LinkedHashMap<>();

        try (
                Reader reader =
                        new InputStreamReader(
                                file.getInputStream(),
                                StandardCharsets.UTF_8
                        );

                CSVParser parser =
                        CSVFormat.DEFAULT.builder()
                                .setDelimiter(';')
                                .setHeader()
                                .setSkipHeaderRecord(true)
                                .setIgnoreEmptyLines(true)
                                .setTrim(true)
                                .build()
                                .parse(reader)
        ) {

            /*
             * Vérification des colonnes obligatoires
             */
            validateHeaders(parser);

            for (CSVRecord record : parser) {

                String jiraId =
                        normalizeJiraId(
                                getValue(record, "Jira ID")
                        );

                /*
                 * Une ligne sans Jira ID n'est pas exploitable.
                 */
                if (jiraId.isBlank()) {
                    continue;
                }

                RcaEntity entity =
                        new RcaEntity();

                entity.setJiraId(jiraId);

                entity.setDescription(
                        getValue(
                                record,
                                "Description"
                        )
                );

                entity.setRca(
                        getValue(
                                record,
                                "RCA"
                        )
                );

                entity.setDefectLeakage(
                        getValue(
                                record,
                                "Defect Leakage"
                        )
                );

                entity.setCorrectiveAction(
                        getValue(
                                record,
                                "Corrective action Preventive action"
                        )
                );

                entity.setPreventiveAction(
                        getValue(
                                record,
                                "Preventive action"
                        )
                );

                entity.setCategory(
                        getValue(
                                record,
                                "Category"
                        )
                );

                entity.setStatus(
                        normalizeStatus(
                                getValue(
                                        record,
                                        "Status"
                                )
                        )
                );

                /*
                 * Un Jira ID = un RCA courant.
                 *
                 * En cas de doublon dans le CSV :
                 * dernière ligne gagnante.
                 */
                uniqueRcas.put(
                        jiraId,
                        entity
                );
            }
        }

        /*
         * IMPORTANT :
         *
         * On ne supprime l'ancien snapshot qu'après
         * avoir réussi à lire et valider le nouveau fichier.
         */
        rcaRepository.deleteAll();

        /*
         * Enregistrement du nouveau snapshot.
         */
        List<RcaEntity> entities =
                new ArrayList<>(
                        uniqueRcas.values()
                );

        rcaRepository.saveAll(entities);

        /*
         * Force l'envoi SQL avant de retourner SUCCESS.
         * Cela permet de détecter immédiatement une éventuelle
         * erreur de contrainte.
         */
        rcaRepository.flush();

        System.out.println(
                "=========================================="
        );
        System.out.println("RCA IMPORT");
        System.out.println(
                "File = " + file.getOriginalFilename()
        );
        System.out.println(
                "Unique RCA imported = "
                        + entities.size()
        );
        System.out.println(
                "Jira IDs = "
                        + entities.stream()
                                .map(RcaEntity::getJiraId)
                                .toList()
        );
        System.out.println(
                "=========================================="
        );

        return entities.size();
    }

    /**
     * Vérifie les colonnes nécessaires.
     */
    private void validateHeaders(
            CSVParser parser) {

        List<String> requiredHeaders =
                List.of(
                        "Jira ID",
                        "Description",
                        "RCA",
                        "Defect Leakage",
                        "Corrective action Preventive action",
                        "Preventive action",
                        "Category",
                        "Status"
                );

        Map<String, Integer> headers =
                parser.getHeaderMap();

        for (String required : requiredHeaders) {

            boolean found =
                    headers.keySet()
                            .stream()
                            .anyMatch(
                                    header ->
                                            cleanHeader(header)
                                                    .equalsIgnoreCase(
                                                            required
                                                    )
                            );

            if (!found) {
                throw new IllegalArgumentException(
                        "Colonne RCA manquante : "
                                + required
                                + ". Colonnes trouvées : "
                                + headers.keySet()
                );
            }
        }
    }

    /**
     * Récupération robuste d'une colonne.
     *
     * Permet notamment de supporter un BOM UTF-8
     * dans le premier header.
     */
    private String getValue(
            CSVRecord record,
            String expectedHeader) {

        for (String actualHeader :
                record.toMap().keySet()) {

            if (cleanHeader(actualHeader)
                    .equalsIgnoreCase(
                            expectedHeader
                    )) {

                String value =
                        record.get(actualHeader);

                return value == null
                        ? ""
                        : value.trim();
            }
        }

        return "";
    }

    /**
     * Nettoyage des noms de colonnes CSV.
     */
    private String cleanHeader(
            String header) {

        if (header == null) {
            return "";
        }

        return header
                .replace("\uFEFF", "")
                .replace("\u00A0", " ")
                .trim();
    }

    /**
     * Normalisation Jira ID.
     *
     * Exemple :
     * " fiscdsol-15265 "
     * ->
     * "FISCDSOL-15265"
     */
    private String normalizeJiraId(
            String jiraId) {

        if (jiraId == null) {
            return "";
        }

        return jiraId
                .replace("\uFEFF", "")
                .replace("\u00A0", " ")
                .trim()
                .toUpperCase(
                        Locale.ROOT
                );
    }

    /**
     * Normalisation légère du statut.
     */
    private String normalizeStatus(
            String status) {

        if (status == null) {
            return "";
        }

        String normalized =
                status
                        .replace("\uFEFF", "")
                        .replace("\u00A0", " ")
                        .trim();

        if (normalized.equalsIgnoreCase("DONE")) {
            return "Done";
        }

        if (normalized.equalsIgnoreCase("IN PROGRESS")) {
            return "In progress";
        }

        if (normalized.equalsIgnoreCase("TO DO")
                || normalized.equalsIgnoreCase("TODO")) {
            return "To Do";
        }

        return normalized;
    }
}