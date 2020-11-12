package utils;


import org.slf4j.Logger;

public class Times {

    public interface Task {
        void execute();
    }

    public static void timeConsuming(boolean isOpen, String title, Logger logger, Task task) {
        if (isOpen) {
            long begin = System.currentTimeMillis();
            task.execute();
            long end = System.currentTimeMillis();
            logger.info("[TLOG] {} 耗时:{}", title, end - begin);
        }
    }
}