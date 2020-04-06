package coen346assignment3.scheduler;

import coen346assignment3.process.Process;

import java.util.Comparator;

public class RemainingTimeComparator implements Comparator<Process> {

    /**
     * Compares remaining times and puts lowest remaining time at head of queue.
     *
     * @param p1 Process 1
     * @param p2 Process 2
     * @return Process has higher or lower remaining time
     */
    @Override
    public int compare(Process p1, Process p2) {
        // If selected process has less time left than the one being compared
        if (p1.getRemainingTime() < p2.getRemainingTime())
            return -1;
            // If selected process has more time left than the one being compared
        else if (p1.getRemainingTime() > p2.getRemainingTime())
            return 1;
        else // If remaining times are the same, prioritize process that has been in system longer
            return Long.compare(p1.getArrivalTime(), p2.getArrivalTime());
    }
}
