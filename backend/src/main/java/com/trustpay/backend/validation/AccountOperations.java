package com.trustpay.backend.validation;

import com.trustpay.backend.entity.User;
import com.trustpay.backend.enums.AccountStatus;
import com.trustpay.backend.exception.UnauthorizedOperationException;

public final class AccountOperations {
    private AccountOperations() {}

    public static void requireActive(User user) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedOperationException("Account must be active for this operation");
        }
    }
}
