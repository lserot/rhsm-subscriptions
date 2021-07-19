package org.candlepin.subscriptions.subscription;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.candlepin.subscriptions.task.TaskQueueProperties;
import org.candlepin.subscriptions.util.KafkaConsumerRegistry;
import org.candlepin.subscriptions.util.SeekableKafkaConsumer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SubscriptionWorker extends SeekableKafkaConsumer {

    @Getter
    int noOfTimesSyncSubsExecuted = 0;

    SubscriptionSyncController subscriptionSyncController;
    protected SubscriptionWorker(
            @Qualifier("subscriptionTasks") TaskQueueProperties taskQueueProperties,
            KafkaConsumerRegistry kafkaConsumerRegistry,
            SubscriptionSyncController subscriptionSyncController) {
        super(taskQueueProperties, kafkaConsumerRegistry);
        this.subscriptionSyncController = subscriptionSyncController;
    }


    @KafkaListener(
            id = "#{__listener.groupId}",
            topics = "#{__listener.topic}",
            containerFactory = "subscriptionSyncListenerContainerFactory")
    public void receive(SyncSubscriptions syncSubscriptions) {
        log.info("Subscription Worker is syncing subs with values: {} ", syncSubscriptions.toString());
        noOfTimesSyncSubsExecuted++;
        subscriptionSyncController.syncSubscriptions(
                syncSubscriptions.getOrgId(), syncSubscriptions.getOffset(), syncSubscriptions.getLimit());
    }
}
