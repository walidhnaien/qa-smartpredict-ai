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
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JiraImportService {

    private final UserStoryRepository userStoryRepository;
    private final ReleaseRepository releaseRepository;

    public int importCsv(MultipartFile file) {

        int count = 0;

        try {
            ReleaseEntity release = releaseRepository
                    .findByVersion("2026.1")
                    .orElseThrow(() -> new RuntimeException("Release 2026.1 not found"));

            Reader reader = new InputStreamReader(file.getInputStream());

            CSVParser csvParser = CSVFormat.DEFAULT
                    .builder()
                    .setDelimiter(';')
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);
			log.info("Headers : {}", csvParser.getHeaderMap().keySet());

            for (CSVRecord record : csvParser) {

                String issueKey = record.get("Issue key");

                if (issueKey == null || issueKey.isBlank()) {
                    continue;
                }

                UserStoryEntity userStory = userStoryRepository
                .findByJiraKey(issueKey)
                .orElseGet(UserStoryEntity::new);

				if (userStory.getId() == null) {
					userStory.setId(UUID.randomUUID());
					userStory.setRelease(release);
					count++;
				}
                userStory.setJiraKey(issueKey);
                userStory.setIssueType(record.get("Issue Type"));
                userStory.setSummary(record.get("Summary"));
                userStory.setStatus(record.get("Status"));
                userStory.setSprint(record.get("Sprint"));
                userStory.setReporter(record.get("Reporter"));
                userStory.setAssignee(record.get("Assignee"));
				userStory.setTsTicketId(record.get("TS tickets ID"));

                userStoryRepository.save(userStory);

                count++;
            }

        } catch (Exception e) {
            log.error("Jira import error", e);
            throw new RuntimeException(e);
        }

        return count;
    }
}