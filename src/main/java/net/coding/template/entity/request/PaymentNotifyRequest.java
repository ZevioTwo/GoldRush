package net.coding.template.entity.request;

import lombok.Data;

@Data
public class PaymentNotifyRequest {
    private String id;
    private String createTime;
    private String eventType;
    private String resourceType;
    private String summary;
    private Resource resource;
    private String timestamp;
    private String nonce;
    private String body;
    private String signature;

    @Data
    public static class Resource {
        private String originalType;
        private String algorithm;
        private String ciphertext;
        private String associatedData;
        private String nonce;
    }
}
