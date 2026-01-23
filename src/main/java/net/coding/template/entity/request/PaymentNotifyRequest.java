package net.coding.template.entity.request;

import lombok.Data;

@Data
public class PaymentNotifyRequest {
    private String id;
    private String create_time;
    private String event_type;
    private String resource_type;
    private Resource resource;

    @Data
    public static class Resource {
        private String algorithm;
        private String ciphertext;
        private String associated_data;
        private String nonce;
        private String original_type;
    }
}
