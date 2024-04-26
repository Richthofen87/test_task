import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.concurrent.*;

@Data
public class CrptApi {

    public static void main(String[] args) {
        HonestSignApi.Root newDoc = HonestSignApi.Root.builder()
                .description(new HonestSignApi.Root.Description("string"))
                .docId("string")
                .docStatus("string")
                .docType("LP_INTRODUCE_GOODS")
                .importRequest(true)
                .ownerInn("string")
                .participantInn("string")
                .producerInn("string")
                .productionDate(LocalDate.of(2020, 1, 23))
                .productionType("string")
                .products(new ArrayList<>() {{
                    add(HonestSignApi.Root.Product.builder()
                            .certificateDocument("string")
                            .certificateDocumentDate(LocalDate.of(2020, 1, 23))
                            .certificateDocumentNumber("string")
                            .ownerInn("string")
                            .producerInn("string")
                            .productionDate(LocalDate.of(2020, 1, 23))
                            .tnvedCode("string")
                            .uitCode("string")
                            .uituCode("string")
                            .build());
                }})
                .regDate(LocalDate.of(2020, 1, 23))
                .regNumber("string")
                .build();
        HonestSignApi honestSignApi = new HonestSignApi(10, TimeUnit.SECONDS);
        honestSignApi.createNewDoc(newDoc);
    }

    @Getter
    public static class HonestSignApi {
        private final String USERNAME = "userName";
        private final String PASSWORD = "password";
        private final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
        private final int requestLimit;
        private final TimeUnit timeUnit;
        private final Semaphore semaphore;
        private ScheduledExecutorService scheduler;

        public HonestSignApi(int requestLimit, TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            this.requestLimit = requestLimit;
            this.semaphore = new Semaphore(requestLimit, true);
            schedulePermit();
        }

        public Root createNewDoc(Root newDoc) {
            try {
                semaphore.acquire();
                ObjectMapper jsonMapper = new ObjectMapper();
                jsonMapper.registerModule(new JavaTimeModule());
                HttpClient client = HttpClient.newBuilder()
                        .authenticator(new Authenticator() {
                            @Override
                            protected PasswordAuthentication getPasswordAuthentication() {
                                return new PasswordAuthentication(USERNAME, PASSWORD.toCharArray());
                            }
                        }).build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(newDoc)))
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                return jsonMapper.readValue(response.body(), Root.class);
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Root {
            private Description description;

            @JsonProperty("doc_id")
            private String docId;

            @JsonProperty("doc_status")
            private String docStatus;

            @JsonProperty("doc_type")
            private String docType;

            private boolean importRequest;

            @JsonProperty("owner_inn")
            private String ownerInn;

            @JsonProperty("participant_inn")
            private String participantInn;

            @JsonProperty("producer_inn")
            private String producerInn;

            @JsonProperty("production_date")
            private LocalDate productionDate;

            @JsonProperty("production_type")
            private String productionType;

            private ArrayList<Product> products;

            @JsonProperty("reg_date")
            private LocalDate regDate;

            @JsonProperty("reg_number")
            private String regNumber;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Description {
                private String participantInn;
            }

            @Data
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Product {
                @JsonProperty("certificate_document")
                private String certificateDocument;

                @JsonProperty("certificate_document_date")
                private LocalDate certificateDocumentDate;

                @JsonProperty("certificate_document_number")
                private String certificateDocumentNumber;

                @JsonProperty("owner_inn")
                private String ownerInn;

                @JsonProperty("producer_inn")
                private String producerInn;

                @JsonProperty("production_date")
                private LocalDate productionDate;

                @JsonProperty("tnved_code")
                private String tnvedCode;

                @JsonProperty("uit_code")
                private String uitCode;

                @JsonProperty("uitu_code")
                private String uituCode;
            }
        }

        private void schedulePermit() {
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> semaphore.release(requestLimit - semaphore.availablePermits()),
                    0, 1, timeUnit);
        }
    }
}
