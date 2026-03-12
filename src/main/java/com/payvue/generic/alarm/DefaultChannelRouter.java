package com.payvue.generic.alarm;

import java.util.HashSet;
import java.util.Set;

final class DefaultChannelRouter implements ChannelRouter {
    private final Set<AlarmChannel> channels = new HashSet<>();
    private final Set<AlarmChannel> customChannels = new HashSet<>();

    @Override
    public void register(AlarmChannel channel) {
        channels.add(channel);
    }

    public void registerCustom(AlarmChannel channel) {
        if (channel != null) {
            customChannels.add(channel);
        }
    }

    @Override
    public void route(Alarm alarm, AlarmContext context) throws SendFailureException {
        if (channels.isEmpty()) {
            throw new SendFailureException("no channel for category: " + alarm.channelKey());
        }

        boolean matched = false;
        for (AlarmChannel candidate : channels) {
            if (candidate.supports(alarm.channelKey())) {
                candidate.send(alarm);
                matched = true;
            }
        }

        if (!matched) {
            throw new SendFailureException("no channel for category: " + alarm.channelKey());
        }

        for (AlarmChannel custom : customChannels) {
            custom.send(alarm);
        }
    }
}
