package assignment_notifer.demo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.springframework.web.bind.annotation.PostMapping;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CanvasFeedController {
    private final AssignmentRepository assignmentRepository;

    public CanvasFeedController(AssignmentRepository assignmentRepository) {
        this.assignmentRepository = assignmentRepository;
    }

    @GetMapping("/api/canvas/feed-preview")
    public Map<String, Object> previewFeed() {
        String feedUrl = System.getenv("CANVAS_ICAL_URL");

        if (feedUrl == null || feedUrl.isBlank()) {
            throw new IllegalStateException("Canvas feed URL is not set.");
        }

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(feedUrl))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                        "Canvas feed returned HTTP " + response.statusCode()
                );
            }

            Calendar calendar = new CalendarBuilder()
                    .build(new StringReader(response.body()));

            List<Map<String, Object>> entries = new ArrayList<>();

            for (Component event : calendar.getComponents(Component.VEVENT)) {
                 String summary = event.getProperty("SUMMARY")
                .map(property -> property.getValue())
                .orElse("(no title)");

                String start = event.getProperty("DTSTART")
                .map(property -> property.getValue())
                .orElse("(no date)");

                 String uid = event.getProperty("UID")
                .map(property -> property.getValue())
                .orElse("(no uid)");

                String url = event.getProperty("URL")
                .map(property -> property.getValue())
                .orElse("(no url)");

                var propertyNames = event.getProperties().stream()
                .map(property -> property.getName())
                .distinct()
                .toList();

                boolean likelyAssignment = event.getProperty("DTEND").isEmpty();

                if (!likelyAssignment) {
                    continue;
                }
                 entries.add(Map.of(
                    "summary", summary,
                    "start", start,
                    "uid", uid,
                    "url", url,
                    "propertyNames", propertyNames,
                    "likelyAssignment", likelyAssignment
    ));
}

            return Map.of(
                    "entriesFound", entries.size(),
                    "entries", entries
            );

        } catch (IOException | ParserException e) {
            throw new RuntimeException("Could not parse the Canvas calendar feed.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Canvas feed request was interrupted.", e);
        }
    }
    @PostMapping("/api/canvas/sync")
@SuppressWarnings("unchecked")
public Map<String, Object> syncAssignments() {
    List<Map<String, Object>> entries =
            (List<Map<String, Object>>) previewFeed().get("entries");

    int processed = 0;

    for (Map<String, Object> entry : entries) {
        String uid = (String) entry.get("uid");
        String summary = (String) entry.get("summary");
        String start = (String) entry.get("start");

        if (uid == null || uid.equals("(no uid)") || start == null || start.equals("(no date)")) {
            continue;
        }

        Assignment assignment = assignmentRepository.findByCanvasUid(uid)
                .orElseGet(Assignment::new);

        assignment.setCanvasUid(uid);
        assignment.setName(assignmentName(summary));
        assignment.setCourse(courseName(summary));
        assignment.setDueDate(parseCanvasDate(start));

        assignmentRepository.save(assignment);
        processed++;
    }

    return Map.of(
            "processed", processed,
            "totalInDatabase", assignmentRepository.count()
    );
}

private String assignmentName(String summary) {
    int courseStart = summary.lastIndexOf(" [");
    if (courseStart >= 0 && summary.endsWith("]")) {
        return summary.substring(0, courseStart);
    }
    return summary;
}

private String courseName(String summary) {
    int courseStart = summary.lastIndexOf(" [");
    if (courseStart >= 0 && summary.endsWith("]")) {
        return summary.substring(courseStart + 2, summary.length() - 1);
    }
    return "Canvas";
}

private LocalDateTime parseCanvasDate(String start) {
    if (start.length() == 8) {
        return LocalDate.parse(start, DateTimeFormatter.BASIC_ISO_DATE)
                .atTime(23, 59);
    }

    if (start.endsWith("Z")) {
        DateTimeFormatter utcFormat =
                DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX");

        return OffsetDateTime.parse(start, utcFormat)
                .atZoneSameInstant(ZoneId.of("America/New_York"))
                .toLocalDateTime();
    }

    return LocalDateTime.parse(
            start,
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    );
}
}