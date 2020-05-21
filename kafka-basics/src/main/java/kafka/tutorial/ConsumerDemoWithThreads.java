package kafka.tutorial;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoWithThreads {

    public static void main(String[] args) {
        new ConsumerDemoWithThreads().run();
    }

    private ConsumerDemoWithThreads(){

    }

    private void run(){
        Logger logger = LoggerFactory.getLogger(ConsumerDemo.class);
        String bootstrapServers = "127.0.0.1:9092";
        String groupId = "my-fourth-app";
        String topic = "first_topic";

        // latch for multiple threads
        CountDownLatch latch = new CountDownLatch(1);

        logger.info("Creating the consumer thread");

        // create consumer runnable
        Runnable myConsumerRunnable = new ConsumerRunnable(latch,
                bootstrapServers,
                groupId,
                topic);

        // start the thread
        Thread thread = new Thread(myConsumerRunnable);
        thread.start();

        // add a shutDown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Caught shutdown hook");
            ((ConsumerRunnable) myConsumerRunnable).shutDown();
            try{
                latch.await();
            } catch (InterruptedException e){
                e.printStackTrace();
            }
            logger.info("Application has exited");
        }
        ));

        try{
            latch.await();
        } catch (InterruptedException e) {
            logger.error("Application got interrupted", e);
        } finally {
            logger.info("Application is closing");
        }
    }


    public class ConsumerRunnable implements Runnable{
        CountDownLatch latch;
        private KafkaConsumer<String, String> consumer;
        private Logger logger = LoggerFactory.getLogger(ConsumerDemo.class);

        public ConsumerRunnable(CountDownLatch latch,
                              String bootstrapServers,
                              String groupId,
                              String topic) {
            this.latch = latch;

            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

            consumer = new KafkaConsumer<String, String>(properties);

            consumer.subscribe(Arrays.asList(topic));
        }

        @Override
        public void run() {
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord record : records) {
                        logger.info("Keys: " + record.key() + ", Value : " + record.value());
                        logger.info("Partition : " + record.partition() + ", Offset : " + record.offset());
                    }
                }
            } catch (WakeupException e) {
                logger.info("Received shutDown signal!");
            }finally {
                consumer.close();

                // done with the consumer
                latch.countDown();
            }
        }

        public void shutDown(){
            // is a method that interrupt consumer.poll()
            consumer.wakeup();
        }
    }
}
