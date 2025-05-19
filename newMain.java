package dao.Mail;

public class newMain {
    private static TimeChecker timeChecker;

    public static void main(String[] args) {
        System.out.println("Khởi động hệ thống kiểm tra thời gian và gửi email thông báo...");

        // Tạo và khởi động TimeChecker với chu kỳ kiểm tra là 1 phút
        timeChecker = new TimeChecker(1);

        // Kiểm tra xem biến môi trường email đã được cấu hình chưa
        String emailUsername = System.getenv("EMAIL_USERNAME");
        String emailPassword = System.getenv("EMAIL_PASSWORD");

        if (emailUsername == null || emailPassword == null) {
            System.err.println("CẢNH BÁO: Biến môi trường EMAIL_USERNAME hoặc EMAIL_PASSWORD chưa được cấu hình!");
            System.err.println("Vui lòng cấu hình biến môi trường để gửi email thông báo.");
        } else {
            System.out.println("Đã cấu hình email thông báo: " + emailUsername);
        }

        // Bắt đầu kiểm tra trong một luồng riêng biệt
        Thread checkerThread = new Thread(() -> {
            timeChecker.startChecking();
        });
        checkerThread.setDaemon(true); // Đặt là luồng daemon để không chặn việc thoát chương trình
        checkerThread.start();

        System.out.println("Hệ thống kiểm tra thời gian đã được khởi động!");
        System.out.println("Sẽ kiểm tra và gửi thông báo mỗi phút...");
    }

    // Phương thức để dừng TimeChecker khi đóng ứng dụng
    public static void stopChecker() {
        if (timeChecker != null) {
            timeChecker.stopChecking();
            System.out.println("Đã dừng hệ thống kiểm tra thời gian.");
        }
    }
}