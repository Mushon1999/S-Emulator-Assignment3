package se.emulator.server;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a user in the S-Emulator system.
 * Contains user information, credit balance, and execution statistics.
 */
public class User {
    private final String name;
    private final AtomicLong credits;
    private final AtomicInteger mainProgramsCount;
    private final AtomicInteger helperFunctionsCount;
    private final AtomicLong totalCreditsUsed;
    private final AtomicInteger executionsPerformed;
    private final long loginTime;
    
    public User(String name, long initialCredits) {
        this.name = name;
        this.credits = new AtomicLong(initialCredits);
        this.mainProgramsCount = new AtomicInteger(0);
        this.helperFunctionsCount = new AtomicInteger(0);
        this.totalCreditsUsed = new AtomicLong(0);
        this.executionsPerformed = new AtomicInteger(0);
        this.loginTime = System.currentTimeMillis();
    }
    
    // Getters
    public String getName() { return name; }
    public long getCredits() { return credits.get(); }
    public int getMainProgramsCount() { return mainProgramsCount.get(); }
    public int getHelperFunctionsCount() { return helperFunctionsCount.get(); }
    public long getTotalCreditsUsed() { return totalCreditsUsed.get(); }
    public int getExecutionsPerformed() { return executionsPerformed.get(); }
    public long getLoginTime() { return loginTime; }
    
    // Credit management
    public boolean deductCredits(long amount) {
        long current = credits.get();
        if (current >= amount) {
            credits.addAndGet(-amount);
            totalCreditsUsed.addAndGet(amount);
            return true;
        }
        return false;
    }
    
    public void addCredits(long amount) {
        credits.addAndGet(amount);
    }
    
    // Statistics updates
    public void incrementMainPrograms() {
        mainProgramsCount.incrementAndGet();
    }
    
    public void incrementHelperFunctions() {
        helperFunctionsCount.incrementAndGet();
    }
    
    public void incrementExecutions() {
        executionsPerformed.incrementAndGet();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return name.equals(user.name);
    }
    
    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", credits=" + credits.get() +
                ", mainPrograms=" + mainProgramsCount.get() +
                ", helperFunctions=" + helperFunctionsCount.get() +
                ", totalCreditsUsed=" + totalCreditsUsed.get() +
                ", executions=" + executionsPerformed.get() +
                '}';
    }
}
