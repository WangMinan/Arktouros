package edu.npu.arktouros.mapper;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import edu.npu.arktouros.mapper.search.SearchMapper;
import edu.npu.arktouros.model.common.ElasticsearchIndex;
import edu.npu.arktouros.model.otel.log.Log;
import edu.npu.arktouros.model.otel.metric.Gauge;
import edu.npu.arktouros.model.otel.structure.Service;
import edu.npu.arktouros.model.otel.trace.Span;
import edu.npu.arktouros.model.vo.R;
import edu.npu.arktouros.util.elasticsearch.ElasticsearchUtil;
import edu.npu.arktouros.util.elasticsearch.pool.ElasticsearchClientPool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * @author : [wangminan]
 * @description : 测试ElasticSearchClient
 */
@SpringBootTest
@Slf4j
@Disabled("no connection") // junit4 @Ignore = junit5 @Disabled
class ElasticSearchClientTest {

    @Resource
    private SearchMapper searchMapper;

    private static final String SERVICE_INDEX = "arktouros-service";
    private static final String LOG_INDEX = "arktouros-log";
    private static final String SPAN_INDEX = "arktouros-span";
    private static final String GAUGE_INDEX = "arktouros-gauge";
    private static final String COUNTER_INDEX = "arktouros-counter";
    private static final String SUMMARY_INDEX = "arktouros-summary";
    private static final String HISTOGRAM_INDEX = "arktouros-histogram";

    private static final List<String> indexList = List.of(SERVICE_INDEX, LOG_INDEX, SPAN_INDEX, GAUGE_INDEX,
            COUNTER_INDEX, SUMMARY_INDEX, HISTOGRAM_INDEX);

    @Test
    void testClientConnect() throws IOException {
        log.info("root path:{}", System.getProperty("user.dir"));
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        BooleanResponse exists =
                esClient.indices().exists(builder -> builder.index("arktouros_log"));
        ElasticsearchClientPool.returnClient(esClient);
        log.info("index exists: {}", exists.value());
    }

    @Test
    void testWriteSame() throws IOException {
        // 只会写一次
        Service service = Service.builder().name("test_service").build();
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        esClient.index(
                builder -> builder
                        .index(SERVICE_INDEX)
                        .id(service.getId())
                        .document(service)
        );
        esClient.index(
                builder -> builder
                        .index(SERVICE_INDEX)
                        .id(service.getId())
                        .document(service)
        );
        ElasticsearchClientPool.returnClient(esClient);
        Assertions.assertNotNull(service);
    }

    @Test
    void testSpanDecode() {
        TermQuery query = new TermQuery.Builder()
                .field("serviceName")
                .value("telemetrygen")
                .build();
        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(ElasticsearchIndex.SPAN_INDEX.getIndexName())
                .query(query._toQuery())
                .build();
        try {
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Span> response = esClient.search(searchRequest, Span.class);
            ElasticsearchClientPool.returnClient(esClient);
            if (response.hits().total() != null) {
                log.info(String.valueOf(response.hits().total().value()));
            }
        } catch (IOException e) {
            log.error("search error:{}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void testLogDecode() throws JsonProcessingException {

        SearchRequest searchRequest = new SearchRequest.Builder()
                .index(ElasticsearchIndex.LOG_INDEX.getIndexName())
                .query(new MatchAllQuery.Builder().build()._toQuery())
//                .sort(sort)
                .build();
        try {
            ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
            SearchResponse<Log> response = esClient.search(searchRequest, Log.class);
            ElasticsearchClientPool.returnClient(esClient);
            if (response.hits().total() != null) {
                log.info(String.valueOf(response.hits().total().value()));
            }
        } catch (IOException e) {
            log.error("search log error:{}", e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    void deleteMappings() {
        log.info("start deleting mappings");
        ElasticsearchClient esClient = ElasticsearchClientPool.getClient();
        indexList.forEach(index -> {
            try {
                esClient.indices().delete(builder -> builder.index(index));
            } catch (IOException e) {
                log.error("delete index:{} failed", index);
            }
        });
        ElasticsearchClientPool.returnClient(esClient);
    }

    @Test
    void getMetricNames() {
        List<String> metricsNames = searchMapper.getMetricsNames("otelcol-contrib", null);
        log.info("metricsNames:{}", metricsNames);
    }

    @Test
    void aggTest() throws IOException {
        R namespaceList = searchMapper.getNamespaceList(null);
        log.info("namespaceList:{}", namespaceList);
        R namespaceList1 = searchMapper.getNamespaceList("def");
        log.info("namespaceList1:{}", namespaceList1);
        R namespaceList2 = searchMapper.getNamespaceList("default");
        log.info("namespaceList2:{}", namespaceList2);
    }

    @Test
    void scrollSearchTest() {
        SearchRequest.Builder searchRequestBuilder =
                new SearchRequest.Builder()
                        .index(ElasticsearchIndex.GAUGE_INDEX.getIndexName())
                        .size(10);
        List<Gauge> gauges = ElasticsearchUtil.scrollSearch(searchRequestBuilder, Gauge.class);
        log.info("result:{}", gauges);
    }

    @Test
    void addMetricToTestService() throws IOException, InterruptedException {
        double[] throughPuts = {2.0, 3.0, 2.0, 5.0, 0.0, 1.0};
        for (double value : throughPuts) {
            Gauge build = Gauge.builder()
                    .name("throughput")
                    .description("Request to service per second")
                    .value(value)
                    .labels(new HashMap<>())
                    .timestamp(System.currentTimeMillis())
                    .build();
            build.setServiceName("test");
            ElasticsearchUtil.sink(
                    ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                    build
            );
            Thread.sleep(1000);
        }
        double[] responseTimes = {50.5, 30.5, 30.0, 40.0, 45.0, 32.5, 44.5};
        for (double value : responseTimes) {
            Gauge build = Gauge.builder()
                    .name("response_time")
                    .description("Response time of service, unit ms.")
                    .value(value)
                    .labels(new HashMap<>())
                    .timestamp(System.currentTimeMillis())
                    .build();
            build.setServiceName("test");
            ElasticsearchUtil.sink(
                    ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                    build
            );
            Thread.sleep(1000);
        }
        double[] errorRates = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        for (double value : errorRates) {
            Gauge build = Gauge.builder()
                    .name("error_rate")
                    .description("Error rate of service")
                    .value(value)
                    .labels(new HashMap<>())
                    .timestamp(System.currentTimeMillis())
                    .build();
            build.setServiceName("test");
            ElasticsearchUtil.sink(
                    ElasticsearchIndex.GAUGE_INDEX.getIndexName(),
                    build
            );
            Thread.sleep(1000);
        }
    }
}
