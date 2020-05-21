package kafka.tutorial3;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ElasticSearchConsumer {

  public static RestHighLevelClient createClient() {

    String hostname = "kafka-begineers-1716286819.eu-west-1.bonsaisearch.net";
    String username = "pmga2imfmi";
    String password = "xbfmjuwxs6";

    // dont do if you run a local Elastic Search
    final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY,
      new UsernamePasswordCredentials(username, password));

    RestClientBuilder builder = RestClient.builder(
      new HttpHost(hostname, 443, "https"))
      .setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
        @Override
        public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpClientBuilder) {
          return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }
      });
    RestHighLevelClient client = new RestHighLevelClient(builder);
    return client;
  }

  public static KafkaConsumer<String, String> createConsumer(String topic){
    String bootstrapServers = "127.0.0.1:9092";
    String groupId = "kafka-demo-elasticsearch";

    Properties properties = new Properties();
    properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false"); // disable auto commit of offset
    properties.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "100");

    KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);
    consumer.subscribe(Arrays.asList(topic));
    return consumer;
  }

  private static com.google.gson.JsonParser jsonParser = new com.google.gson.JsonParser();

  private static String extractIdFromTweet(String tweetJson){
    // gson library
    return jsonParser.parse(tweetJson).getAsJsonObject().get("id_str").getAsString();
  }

  public static void main(String[] args) throws IOException {
    // create logger
    Logger logger = LoggerFactory.getLogger(ElasticSearchConsumer.class.getName());

    RestHighLevelClient client = createClient();

    KafkaConsumer<String, String> consumer = createConsumer("twitter_tweets");
    while(true){
      ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

      int recordCount = records.count();
      logger.info("Received "+recordCount+" records");

      BulkRequest bulkRequest = new BulkRequest();

      for (ConsumerRecord<String, String> record: records){
        // two ways of creating id
        // kafka generic id
        // String id = record.topic() +"_"+ record.partition() +"_"+ record.offset();

        // make request and catch an id that is null
        try{
          // twitter feed specific id
          String id  = extractIdFromTweet(record.value());
          // where data is inserted into elastic search
          IndexRequest indexRequest = new IndexRequest(
            "twitter",
            "tweets",
             id // make consumer idempotent
          ).source(record.value(), XContentType.JSON);

          bulkRequest.add(indexRequest); // add to bulk request (takes no time)
        }catch (NullPointerException e){
          logger.warn("Skipping bad data : " + record.value());
        }
//        IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
//        logger.info(indexResponse.getId());
//        try{
//          Thread.sleep(10); // a small delay
//        }catch (InterruptedException e){
//          e.printStackTrace();
//        }
      }
      if (recordCount > 0){
        BulkResponse bulkItemResponses = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        logger.info("Committing offsets...");
        consumer.commitSync();
        logger.info("offsets have been committed");
        try{
          Thread.sleep(1000); // a small delay
        }catch (InterruptedException e){
          e.printStackTrace();
        }
      }
    }
    // close the client
    // client.close();
  }
}
