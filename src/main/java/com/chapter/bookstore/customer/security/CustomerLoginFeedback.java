package com.chapter.bookstore.customer.security;

public record CustomerLoginFeedback(String email, String message) implements java.io.Serializable {}
