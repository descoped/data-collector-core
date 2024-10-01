package io.descoped.dc.core.health;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.descoped.dc.api.health.HealthRenderPriority;
import io.descoped.dc.api.health.HealthResource;
import io.descoped.dc.api.util.JsonParser;
import io.descoped.dc.core.executor.WorkerException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@HealthRenderPriority(priority = 15)
public class HealthWorkerHistoryResource implements HealthResource {

    static final int MAX_HISTORY_CAPACITY = 25;

    final Lock lock = new ReentrantLock();
    final CopyOnWriteArrayList<ObjectNode> historyList = new CopyOnWriteArrayList<>();

    @Override
    public Optional<Boolean> isUp() {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "worker-history";
    }

    @Override
    public boolean isList() {
        return true;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return true;
    }

    @Override
    public Object resource() {
        List<ObjectNode> reversedList = new ArrayList<>(historyList);
        Collections.reverse(reversedList);
        return reversedList;
    }

    public void add(HealthWorkerResource workerResource) {
        try {
            lock.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new WorkerException(e);
        }
        try {
            JsonParser jsonParser = JsonParser.createJsonParser();
            ObjectNode convertedNode = jsonParser.mapper().convertValue(workerResource.resource(), ObjectNode.class);
            if (historyList.size() > MAX_HISTORY_CAPACITY) {
                historyList.remove(historyList.size() - 1);
            }
            historyList.add(convertedNode);

        } finally {
            lock.unlock();
        }
    }
}
