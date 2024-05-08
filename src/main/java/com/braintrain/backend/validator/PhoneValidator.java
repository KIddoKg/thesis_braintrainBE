package com.braintrain.backend.validator;

import org.springframework.stereotype.Service;

import java.util.function.Predicate;

@Service
public class PhoneValidator implements Predicate<String> {
    @Override
    public boolean test(String phone) {
        return phone.length() == 12 && phone.startsWith("+84");
    }
}
