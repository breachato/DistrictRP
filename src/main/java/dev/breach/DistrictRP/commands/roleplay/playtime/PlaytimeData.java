package dev.breach.DistrictRP.commands.roleplay.playtime;

public class PlaytimeData {

    private long totalSeconds;
    private long dailySeconds;
    private long weeklySeconds;
    private long monthlySeconds;

    private long dailyReset;
    private long weeklyReset;
    private long monthlyReset;

    public PlaytimeData() {}

    public long getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(long totalSeconds) { this.totalSeconds = totalSeconds; }
    public long getDailySeconds() { return dailySeconds; }
    public void setDailySeconds(long dailySeconds) { this.dailySeconds = dailySeconds; }
    public long getWeeklySeconds() { return weeklySeconds; }
    public void setWeeklySeconds(long weeklySeconds) { this.weeklySeconds = weeklySeconds; }
    public long getMonthlySeconds() { return monthlySeconds; }
    public void setMonthlySeconds(long monthlySeconds) { this.monthlySeconds = monthlySeconds; }
    public long getDailyReset() { return dailyReset; }
    public void setDailyReset(long dailyReset) { this.dailyReset = dailyReset; }
    public long getWeeklyReset() { return weeklyReset; }
    public void setWeeklyReset(long weeklyReset) { this.weeklyReset = weeklyReset; }
    public long getMonthlyReset() { return monthlyReset; }
    public void setMonthlyReset(long monthlyReset) { this.monthlyReset = monthlyReset; }

    public void addSecond() {
        long now = System.currentTimeMillis();
        totalSeconds++;
        if (now - dailyReset > 24L * 60 * 60 * 1000) { dailySeconds = 0; dailyReset = now; }
        if (now - weeklyReset > 7L * 24 * 60 * 60 * 1000) { weeklySeconds = 0; weeklyReset = now; }
        if (now - monthlyReset > 30L * 24 * 60 * 60 * 1000) { monthlySeconds = 0; monthlyReset = now; }
        dailySeconds++;
        weeklySeconds++;
        monthlySeconds++;
    }
}