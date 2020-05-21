package kafka.tutorial;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class ProducerDemoKeys {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // create a logger
        final Logger logger = LoggerFactory.getLogger(ProducerDemoWithCallback.class);

        // save the port to a variable
        String bootstrapServers = "127.0.0.1:9092";

        // instantiate a properties object
        Properties properties = new Properties();

        // ser producer properties
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create a new kafka producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        for (int i = 1; i <= 10; i++){

            String topic = "first_topic";
            String value = "hello_world " + Integer.toString(i);
            String key = "id_" + Integer.toString(i);

            // create a new kafka producer record with topic name and value
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);

            int finalI = i;

            logger.info("key : " + key); // log the key

            // send data - asynchronous
            producer.send(record, new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception == null){
                        logger.info("\nS/N : " + finalI + "\n" +
                                "key : " + key + "\n" +
                                "Received new metadata. \n" +
                                "Topic : " + metadata.topic() + "\n" +
                                "Partition : " + metadata.partition() + "\n" +
                                "Offset : " + metadata.offset() + "\n" +
                                "Timestamp : " + metadata.timestamp());
                    }else{
                        logger.error("Error while producing");
                    }
                }
            }).get(); // block the send() to make it synchronous - dont do this in production
        }

        // flush data
        producer.flush();

        // close producer
        producer.close();
    }

}
