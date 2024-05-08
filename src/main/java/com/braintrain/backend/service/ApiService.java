package com.braintrain.backend.service;

import com.braintrain.backend.api.Api;
import com.braintrain.backend.api.Error;
import com.braintrain.backend.api.Metadata;
import org.springframework.stereotype.Service;

@Service
public class ApiService {
    public <T> Api<T> generateApi(String code, String message, T data) {
        return new Api<>(
                new Metadata(false, 0, 0, 0),
                new Error(code, message),
                data
        );
    }

    public <T> Api<T> generatePaginationApi(T data, long totalElements, int pageNumber, int pageSize) {
        return new Api<>(
                new Metadata(true, totalElements, pageNumber, pageSize),
                null,
                data
        );
    }

    public <T> Api<T> generateSuccessApi(T data) {
        return new Api<>(
                new Metadata(true, 0, 0, 0),
                null,
                data);
    }

    public Api<Void> generateFailApi(String code, String message) {
        return new Api<>(
                new Metadata(false, 0, 0, 0),
                new Error(code, message),
                null
        );
    }
}
