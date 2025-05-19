package dao.Mail;

import java.time.LocalDateTime;

public class EventInfo {
    public int id;
    public String title;
    public String location;
    public LocalDateTime startTime;
    public LocalDateTime endTime;
    public String email;
    public int remindBefore;
    public int occurrences;
    public int intervalMinutes;
    public String repeatCycle;
    public String description;
}
