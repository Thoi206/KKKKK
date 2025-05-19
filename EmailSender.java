package dao.Mail;

import dao.DatabaseConnection;

import javax.mail.*;
import javax.mail.internet.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class EmailSender {
    private final Session mailSession;

    // Inner class to hold event information
    public static class EventInfo {
        public int id;
        public String title;
        public String location;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        public String email;
        public int remindBefore;
        public String description;
        public String repeatCycle;
        public int intervalMinutes;
        public int occurrences;
    }

    public Session getSession() {
        return mailSession;
    }

    public EmailSender() {
        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        // Lấy thông tin từ biến môi trường
        String email = System.getenv("EMAIL_USERNAME");
        String password = System.getenv("EMAIL_PASSWORD");

        mailSession = Session.getDefaultInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(email, password);
            }
        });
    }

    // Phương thức lấy các sự kiện cần gửi thông báo dựa trên thời gian hiện tại
    public List<EventInfo> getEventsToNotify(LocalDateTime currentTime) {
        List<EventInfo> events = new ArrayList<>();

        // Cải thiện câu truy vấn để tìm chính xác các sự kiện cần nhắc nhở
        String sql = "SELECT id, title, location, startTime, endTime, emailNotification, remindBefore, " +
                "numberOfOccurrences, intervalMinutes, repeatCycle, Description, user_id " +
                "FROM Schedule1 " +
                "WHERE numberOfOccurrences > 0 AND " +
                "DATEADD(MINUTE, -remindBefore, startTime) <= ? AND " +
                "startTime > DATEADD(MINUTE, -remindBefore, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Tham số truy vấn
            Timestamp currentTimestamp = Timestamp.valueOf(currentTime);
            stmt.setTimestamp(1, currentTimestamp);
            stmt.setTimestamp(2, currentTimestamp);

            System.out.println("Checking events at current time: " + currentTime);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    EventInfo info = new EventInfo();
                    info.id = rs.getInt("id");
                    info.title = rs.getString("title");
                    info.location = rs.getString("location");
                    info.startTime = rs.getTimestamp("startTime").toLocalDateTime();
                    info.endTime = rs.getTimestamp("endTime").toLocalDateTime();
                    info.email = rs.getString("emailNotification");
                    info.remindBefore = rs.getInt("remindBefore");
                    info.description = rs.getString("Description");
                    info.repeatCycle = rs.getString("repeatCycle");
                    info.intervalMinutes = rs.getInt("intervalMinutes");
                    info.occurrences = rs.getInt("numberOfOccurrences");

                    // Tính toán thời điểm cần gửi thông báo
                    LocalDateTime notifyTime = info.startTime.minusMinutes(info.remindBefore);

                    // Chỉ thêm sự kiện nếu thời điểm hiện tại trong khoảng 1 phút so với thời điểm cần thông báo
                    if (currentTime.isAfter(notifyTime.minusMinutes(1)) &&
                            currentTime.isBefore(notifyTime.plusMinutes(1))) {
                        events.add(info);
                        System.out.println("Found event to notify: " + info.title +
                                " at " + info.startTime +
                                " (notify at: " + notifyTime + ")");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return events;
    }

    // Gửi email thông báo cho các sự kiện tại thời điểm hiện tại
    public void sendEmailsForTime(LocalDateTime currentTime) {
        List<EventInfo> events = getEventsToNotify(currentTime);
        System.out.println("Found " + events.size() + " events to notify at " + currentTime);

        for (EventInfo e : events) {
            // Format thời gian để hiển thị đẹp hơn
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String startTimeStr = e.startTime.format(formatter);
            String endTimeStr = e.endTime.format(formatter);

            String subject = "Nhắc nhở sự kiện sắp bắt đầu: " + e.title;
            String body = "<b>Tiêu đề:</b> " + e.title + "<br>"
                    + "<b>Địa điểm:</b> " + e.location + "<br>"
                    + "<b>Thời gian:</b> " + startTimeStr + " → " + endTimeStr + "<br>"
                    + "<b>Mô tả:</b> " + e.description + "<br><br>"
                    + "Số lần nhắc còn lại: " + (e.occurrences - 1);

            try {
                MimeMessage msg = new MimeMessage(mailSession);
                msg.setFrom(new InternetAddress(System.getenv("EMAIL_USERNAME")));
                msg.setRecipient(Message.RecipientType.TO, new InternetAddress(e.email));
                msg.setSubject(subject);
                msg.setContent(body, "text/html; charset=UTF-8");
                Transport.send(msg);
                System.out.println("Đã gửi email nhắc nhở đến: " + e.email + " cho sự kiện: " + e.title);

                // Cập nhật số lần lặp lại và thời gian mới
                updateOccurrenceCount(e.id, e.occurrences - 1);

                // Nếu còn lần lặp lại, cập nhật thời gian bắt đầu mới dựa trên chu kỳ lặp
                if (e.occurrences - 1 > 0 && !e.repeatCycle.equals("Không lặp lại")) {
                    updateNextOccurrence(e);
                }
            } catch (MessagingException ex) {
                ex.printStackTrace();
            }
        }
    }

    // Cập nhật thông tin cho lần lặp tiếp theo dựa vào loại chu kỳ
    private void updateNextOccurrence(EventInfo event) {
        LocalDateTime newStartTime = event.startTime;
        LocalDateTime newEndTime = event.endTime;

        // Tính toán thời gian mới dựa trên chu kỳ lặp
        switch (event.repeatCycle) {
            case "Hàng ngày":
                newStartTime = event.startTime.plusDays(1);
                newEndTime = event.endTime.plusDays(1);
                break;
            case "Hàng tuần":
                newStartTime = event.startTime.plusWeeks(1);
                newEndTime = event.endTime.plusWeeks(1);
                break;
            case "Hàng tháng":
                newStartTime = event.startTime.plusMonths(1);
                newEndTime = event.endTime.plusMonths(1);
                break;
            default:
                // Sử dụng khoảng thời gian nếu là lặp tùy chỉnh
                newStartTime = event.startTime.plusMinutes(event.intervalMinutes);
                newEndTime = event.endTime.plusMinutes(event.intervalMinutes);
                break;
        }

        updateEventTimes(event.id, newStartTime, newEndTime);
    }

    // Cập nhật thời gian bắt đầu và kết thúc của sự kiện
    private void updateEventTimes(int id, LocalDateTime newStart, LocalDateTime newEnd) {
        String sql = "UPDATE Schedule1 SET startTime = ?, endTime = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(newStart));
            stmt.setTimestamp(2, Timestamp.valueOf(newEnd));
            stmt.setInt(3, id);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Updated event times for event ID " + id + ": " + rowsAffected + " rows affected");
            System.out.println("New start time: " + newStart);
            System.out.println("New end time: " + newEnd);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Cập nhật số lần lặp lại
    private void updateOccurrenceCount(int id, int newCount) {
        String sql = "UPDATE Schedule1 SET numberOfOccurrences = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, newCount);
            stmt.setInt(2, id);
            int rowsAffected = stmt.executeUpdate();
            System.out.println("Updated occurrence count for event ID " + id + " to " + newCount +
                    ": " + rowsAffected + " rows affected");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}