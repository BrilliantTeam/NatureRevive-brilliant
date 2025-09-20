package engineer.skyouo.plugins.naturerevive.common.structs;

import java.util.*;

public class Queue<T> {
    private java.util.Queue<T> taskQueue;

    public Queue() {
        taskQueue = new ArrayDeque<>();
    }

    public Queue(java.util.Queue<T> queue) { taskQueue = queue; }

    public void add(T task) {
        taskQueue.offer(task);
    }

    public T pop() {
        return taskQueue.poll();
    }

    public boolean hasNext() {
        return !taskQueue.isEmpty();
    }

    public int size() {
        return taskQueue.size();
    }

    public Iterator<T> iterator() { return taskQueue.iterator(); }

    public void load(java.util.Queue<T> queue) { taskQueue = queue; }

    public Queue<T> clone() {
        if (taskQueue instanceof ArrayDeque<T> aDeque) {
            return new Queue<>(aDeque.clone());
        } else if (taskQueue instanceof PriorityQueue<T> pQueue) {
            return new Queue<>(new PriorityQueue<>(pQueue));
        }

        return null;
    }
}
