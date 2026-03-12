package com.payvue.generic.alarm;

final class FailureHandler implements AlarmExceptionHandler<SendFailureException> {
    @Override
    public void handle(Alarm alarm, SendFailureException exception, AlarmContext context) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                context.router().route(alarm, context);
                return;
            } catch (SendFailureException retryException) {
                attempts++;
            }
        }
        alarm.markAsFailed(exception.getMessage());
    }
}
