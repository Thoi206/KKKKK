package dao.Mail;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeChecker {
    private int checkIntervalMinutes;
    private volatile boolean running = true;
    private EmailSender emailSender;

    public TimeChecker(int checkIntervalMinutes) {
        this.checkIntervalMinutes = checkIntervalMinutes;
        this.emailSender = new EmailSender();
    }

    public void startChecking() {
        System.out.println("Bắt đầu kiểm tra thời gian mỗi " + checkIntervalMinutes + " phút.");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

        while (running) {
            try {
                LocalDateTime now = LocalDateTime.now();
                System.out.println("Kiểm tra email tại: " + now.format(formatter));

                // Gửi email cho các sự kiện cần thông báo tại thời điểm hiện tại
                emailSender.sendEmailsForTime(now);

                // Ngủ trong khoảng thời gian được chỉ định (tính bằng mili giây)
                Thread.sleep(checkIntervalMinutes * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            } catch (Exception e) {
                System.err.println("Lỗi trong quá trình kiểm tra thời gian: " + e.getMessage());
                e.printStackTrace();

                // Tiếp tục kiểm tra ngay cả khi có lỗi xảy ra
                try {
                    Thread.sleep(60 * 1000); // Ngủ 1 phút nếu có lỗi
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }
    }

    public void stopChecking() {
        running = false;
    }
}