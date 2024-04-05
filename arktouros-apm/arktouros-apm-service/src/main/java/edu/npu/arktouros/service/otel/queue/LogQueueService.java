package edu.npu.arktouros.service.otel.queue;


import edu.npu.arktouros.mapper.otel.queue.LogQueueMapper;
import edu.npu.arktouros.model.queue.LogQueueItem;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author : [wangminan]
 * @description : log队列服务
 */
@Service
@Slf4j
public class LogQueueService implements QueueService<LogQueueItem> {

    @Resource
    private LogQueueMapper queueMapper;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    @Override
    public void put(LogQueueItem logQueueItem) {
        queueMapper.add(logQueueItem);
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public LogQueueItem get() {
        return getItem(true);
    }

    @Override
    public LogQueueItem get(boolean removeAtSameTime) {
        return getItem(removeAtSameTime);
    }

    @SneakyThrows
    private LogQueueItem getItem(boolean removeAtSameTime) {
        LogQueueItem item;
        lock.lock();
        try {
            while ((item = queueMapper.getTop()) == null) {
                notEmpty.await();
            }
            if (removeAtSameTime) {
                queueMapper.removeTop();
            }
        } finally {
            lock.unlock();
        }
        return item;
    }

    @Override
    public boolean isEmpty() {
        return queueMapper.isEmpty();
    }

    @Override
    public long size() {
        return queueMapper.getSize();
    }
}