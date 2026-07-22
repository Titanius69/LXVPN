package com.titanius.lxvpn.core.antivpn.iprestrict;

import com.titanius.lxvpn.core.managers.LogManager;
import com.titanius.lxvpn.core.util.AsyncExecutor;
import com.titanius.lxvpn.core.util.CIDRUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Binds an account to a set of permitted addresses.
 *
 * <p>Aimed at staff accounts. Anti-VPN protects the server from strangers; this protects specific
 * accounts from being used by anyone but their owner, which matters far more for an account that can
 * ban people than for an ordinary player.
 *
 * <p>Entries accept CIDR ranges as well as single addresses, because most home connections do not
 * have a static address and locking an admin out of their own server at three in the morning is a
 * worse outcome than the attack this prevents.
 */
public class IpRestrictManager {

    private final Path file;
    private final LogManager log;
    private final Map<String, Set<String>> bindings = new ConcurrentHashMap<>();
    private final AtomicBoolean writeInFlight = new AtomicBoolean();
    private volatile boolean dirty;
    private volatile boolean enabled;

    public IpRestrictManager(Path dataFolder, LogManager log, boolean enabled) {
        this.log = log;
        this.enabled = enabled;
        this.file = dataFolder.resolve("ip-restrictions.json");
        load();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static String key(String username) {
        return username == null ? "" : username.toLowerCase(Locale.ROOT);
    }

    private void load() {
        if (Files.notExists(file)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JSONObject root = new JSONObject(new JSONTokener(reader));
            for (String name : root.keySet()) {
                Set<String> addresses = ConcurrentHashMap.newKeySet();
                JSONArray array = root.optJSONArray(name);
                if (array != null) {
                    for (int i = 0; i < array.length(); i++) {
                        addresses.add(array.getString(i));
                    }
                }
                bindings.put(key(name), addresses);
            }
            log.general("Loaded IP restrictions for " + bindings.size() + " account(s).");
        } catch (Exception ex) {
            log.warn("Could not read ip-restrictions.json: " + ex.getMessage());
        }
    }

    /**
     * @return true when this account may connect from this address; always true when the account has
     *         no bindings, since an unlisted account is simply unrestricted
     */
    public boolean isAllowed(String username, String ip) {
        if (!enabled) {
            return true;
        }
        Set<String> allowed = bindings.get(key(username));
        if (allowed == null || allowed.isEmpty()) {
            return true;
        }
        for (String entry : allowed) {
            if (entry.equals(ip) || (CIDRUtils.isRange(entry) && CIDRUtils.matches(entry, ip))) {
                return true;
            }
        }
        log.vpn(username + " refused from " + ip + ": address is not on the account's allow list");
        return false;
    }

    public boolean isRestricted(String username) {
        Set<String> allowed = bindings.get(key(username));
        return allowed != null && !allowed.isEmpty();
    }

    public Set<String> addressesOf(String username) {
        Set<String> allowed = bindings.get(key(username));
        return allowed == null ? Set.of() : Collections.unmodifiableSet(allowed);
    }

    public boolean add(String username, String address) {
        boolean added = bindings.computeIfAbsent(key(username), ignored -> ConcurrentHashMap.newKeySet())
                .add(address);
        if (added) {
            markDirty();
        }
        return added;
    }

    public boolean remove(String username, String address) {
        Set<String> allowed = bindings.get(key(username));
        if (allowed == null || !allowed.remove(address)) {
            return false;
        }
        if (allowed.isEmpty()) {
            bindings.remove(key(username));
        }
        markDirty();
        return true;
    }

    public boolean clear(String username) {
        if (bindings.remove(key(username)) == null) {
            return false;
        }
        markDirty();
        return true;
    }

    public int accountCount() {
        return bindings.size();
    }

    private void markDirty() {
        dirty = true;
        if (!writeInFlight.compareAndSet(false, true)) {
            return;
        }
        AsyncExecutor.get().execute(() -> {
            try {
                do {
                    dirty = false;
                    write();
                } while (dirty);
            } finally {
                writeInFlight.set(false);
                if (dirty) {
                    markDirty();
                }
            }
        });
    }

    /**
     * Writes to a temporary file and moves it into place.
     *
     * <p>A crash halfway through a direct write leaves a truncated file, and a truncated allow list
     * means every restricted account is locked out on the next start.
     */
    private void write() {
        JSONObject root = new JSONObject();
        bindings.forEach((name, addresses) -> root.put(name, new JSONArray(addresses)));
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writer.write(root.toString(2));
            }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.warn("Could not save ip-restrictions.json: " + ex.getMessage());
        }
    }
}
