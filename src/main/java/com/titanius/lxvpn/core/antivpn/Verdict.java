package com.titanius.lxvpn.core.antivpn;

/**
 * The outcome of checking one connection.
 *
 * <p>Carries the reason as well as the decision, so the kick screen, the log line and the webhook all
 * say the same thing. Nothing frustrates an operator more than a player reporting a block that the
 * log cannot explain.
 */
public record Verdict(boolean allowed, Reason reason, int score) {

    public enum Reason {
        ALLOWED("allowed"),
        BYPASSED("on the bypass list"),
        BLACKLISTED("previously blacklisted"),
        VPN("VPN, proxy or Tor"),
        COUNTRY("country not permitted"),
        IP_RESTRICTED("account is locked to another address");

        private final String description;

        Reason(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    public static Verdict allow() {
        return new Verdict(true, Reason.ALLOWED, 0);
    }

    public static Verdict bypass() {
        return new Verdict(true, Reason.BYPASSED, 0);
    }

    public static Verdict deny(Reason reason, int score) {
        return new Verdict(false, reason, score);
    }

    /** Which message key the platform layer should use for the kick screen. */
    public String messageKey() {
        return switch (reason) {
            case COUNTRY -> "country-blocked";
            case IP_RESTRICTED -> "ip-restricted";
            default -> "vpn-blocked";
        };
    }
}
