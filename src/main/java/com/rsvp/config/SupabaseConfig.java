//package com.rsvp.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class SupabaseConfig {
//
//    @Value("${supabase.url}")
//    private String supabaseUrl;
//
//    @Value("${supabase.service-key}")
//    private String serviceKey;
//
//    @Bean
//    public SupabaseClient supabaseClient() {
//        return new SupabaseClient(supabaseUrl, serviceKey);
//    }
//}