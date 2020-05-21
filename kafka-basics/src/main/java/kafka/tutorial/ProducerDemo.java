package kafka.tutorial;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class ProducerDemo {
    public static void main(String[] args) {
        // save the port to a variable
        String bootstrapServers = "127.0.0.1:9092";

        // instantiate a properties object
        Properties properties = new Properties();

        // set producer properties
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create a new kafka producer
        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        // create a new kafka producer record with topic name and value
        ProducerRecord<String, String> record = new ProducerRecord<>("first_topic", "hello world");

        // send data - asynchronous producer.send(record);
        producer.send(record);

        // flush data
        producer.flush();

        // close producer
        producer.close();
    }
}
