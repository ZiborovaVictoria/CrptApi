package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ScheduledExecutorService scheduler;
    private final ReentrantLock lock;
    private final AtomicInteger requestCount;
    private final int requestLimit;
    private final TimeUnit timeUnit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.lock = new ReentrantLock(true);
        this.requestCount = new AtomicInteger(0);
        this.requestLimit = requestLimit;
        this.timeUnit = timeUnit;

        this.scheduler.scheduleAtFixedRate(() -> requestCount.set(0), 0, 1, timeUnit);
    }

    public void createDocument(Document document, String signature) throws IOException, InterruptedException {
        lock.lock();
        try {
            while (requestCount.get() >= requestLimit) {
                lock.unlock();
                Thread.sleep(timeUnit.toMillis(1));
                lock.lock();
            }
            requestCount.incrementAndGet();
        } finally {
            lock.unlock();
        }

        String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        String requestBody = objectMapper.writeValueAsString(document);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Signature", signature)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to create document: " + response.body());
        }
    }

    public static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type = "LP_INTRODUCE_GOODS";
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public static class Description {
            public String participantInn;
        }

        public static class Product {
            public String certificate_document;
            public String certificate_document_date;
            public String certificate_document_number;
            public String owner_inn;
            public String producer_inn;
            public String production_date;
            public String tnved_code;
            public String uit_code;
            public String uitu_code;
        }
    }

    public static void main(String[] args) {
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 10);

        Document document = new Document();
        document.description = new Document.Description();
        document.description.participantInn = "example";
        document.doc_id = "doc_id";
        document.doc_status = "doc_status";
        document.importRequest = true;
        document.owner_inn = "owner_inn";
        document.participant_inn = "participant_inn";
        document.producer_inn = "producer_inn";
        document.production_date = "2020-01-23";
        document.production_type = "production_type";
        document.products = new Document.Product[1];
        document.products[0] = new Document.Product();
        document.products[0].certificate_document = "certificate_document";
        document.products[0].certificate_document_date = "2020-01-23";
        document.products[0].certificate_document_number = "certificate_document_number";
        document.products[0].owner_inn = "owner_inn";
        document.products[0].producer_inn = "producer_inn";
        document.products[0].production_date = "2020-01-23";
        document.products[0].tnved_code = "tnved_code";
        document.products[0].uit_code = "uit_code";
        document.products[0].uitu_code = "uitu_code";
        document.reg_date = "2020-01-23";
        document.reg_number = "reg_number";

        try {
            api.createDocument(document, "signature");
            System.out.println("Document created successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
