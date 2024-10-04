package dev.DatxCute.utils;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import dev.DatxCute.Main;

public class CountdownTimer {

    private final int countdownSeconds;
    private final Runnable onFinish;
    private int taskId = -1;
    private boolean running = false;

    public CountdownTimer(int countdownSeconds, Runnable onFinish) {
        this.countdownSeconds = countdownSeconds;
        this.onFinish = onFinish;
    }

    public void start() {
        if (running) return;

        running = true;
        taskId = new BukkitRunnable() {
            int timeLeft = countdownSeconds;

            @Override
            public void run() {
                if (timeLeft <= 0) {
                    onFinish.run();
                    cancel();
                    return;
                }

                timeLeft--;
            }
        }.runTaskTimer(Main.getInstance(), 20L, 20L).getTaskId(); // Chạy mỗi giây (20 ticks)
    }

    public void cancel() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
            running = false;
        }
    }

    public boolean isRunning() {
        return running;
    }
}
