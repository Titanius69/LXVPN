# LXVPN

NOTE: 1.0-BETA refactor wasn't made by me, it was made by Claude Opus 5.0, but before that it was made by me, i was just too lazy to refactor.

**Anti-VPN, anti-proxy and anti-Tor for BungeeCord and Velocity.** One jar, both
proxies, identical behaviour.

Free and open source. No paid tier, no locked features.

> **1.0-BETA.** Tested on Velocity. The BungeeCord side runs the same core and
> the same checks but has not had a test pass yet — run it there if you are
> willing to report what breaks, not on a production network you cannot afford
> to have misbehave.

```
Velocity 3.3+ or BungeeCord 1.20+  ·  Java 17+  ·  no database  ·  no API key required
```

---

## Quick start

1. Drop `LXVPN-1.0-BETA.jar` into `plugins/` on your proxy
2. Restart
3. Run `/lxvpn`

That is the whole setup. It works with no configuration and no API keys — the
strongest signal it uses needs neither.

---

## What it does

Scores every connecting address against up to seven tiers of reputation sources
and refuses the connection when the total crosses a threshold.

**The threshold is the design, not a detail.** No single source can block a
player on its own at the default setting. Free reputation APIs disagree with each
other constantly, and a plugin that blocks because one API said "proxy" will
refuse real players every week — after which the operator uninstalls it rather
than debugging it.

Every source fails to zero. A provider being down, slow or rate limited reduces
accuracy; it never blocks a login and never denies one. There is a hard timeout
on top, because a frozen connecting screen is a worse outcome for a real player
than an unchecked VPN user is for you.

### The signals, roughly by usefulness

**Datacenter ASN** — the strongest one, and free. Commercial VPNs rent capacity
from a handful of hosting companies, and no consumer product puts a residential
player on Amazon, OVH or Hetzner. Local GeoLite2 database, downloaded once: no
key, no rate limit, no outbound request while a player waits. It keeps working
when the APIs are throttled.

**Published VPN and Tor ranges** — facts from the operators themselves rather
than a third party's inference.

**Commercial APIs** — the most accurate per-address, and the only sources needing
a key. Skipped silently when a key is missing.

**Open-proxy lists** — unambiguous. An address advertised publicly as an open
SOCKS proxy is not somebody's home connection.

**Botnet lists** — weakest, and included with a caveat: a home machine infected
with malware appears on these, and the person sitting at it is a real player. One
point each, never decisive alone.

### Also included

- **Country filtering** from a local database, blacklist or whitelist mode
- **Per-account IP binding**, aimed at staff accounts, with CIDR support
- **Discord webhook** notifications, with the address partly masked
- **Virtual threads** for all blocking I/O on Java 21+, detected at runtime

---

## What it does not do

**No anti-bot.** No captcha, no limbo, no connection rate limiting, no behavioural
analysis. That work lives in LuminShield, a separate paid plugin of ours on
BuiltByBit that grew out of this codebase.

This is deliberate. The two solve different problems on different schedules, and
a network that wants VPN blocking should not have to run a bot filter to get it.

**No bundled IP databases.** LXVPN ships no address lists inside the jar. A
list frozen at build time is stale the week after release and cannot be corrected
without a new one. Everything is fetched and refreshed at runtime.

---

## Commands

`/lxvpn`, aliases `/antivpn` and `/lxantivpn`. Permission: `lxvpn.admin`.

| Command | Does |
| --- | --- |
| `/lxvpn` | Status: levels, threshold, counters, database and cache state |
| `/lxvpn check <ip\|player>` | Scores an address now, ignoring the cache |
| `/lxvpn blacklist <add\|remove\|size> [ip]` | Manual entries |
| `/lxvpn iprestrict <add\|remove\|clear\|info> <player> [ip]` | Account bindings |
| `/lxvpn reload` | Rereads config; keeps the old one if the new file has a typo |

`check` bypasses the cache on purpose — the reason anyone runs it is that they
disagree with a cached verdict.

Tab completion works on both proxies.

---

## Configuration

Everything lives in `plugins/LXVPN/config.yml`. The two settings worth
understanding:

**`min-score`** — points needed to refuse a connection. `2` is balanced, `3` is
cautious, `1` is aggressive and you will hear about it. Leave it at the default
for a week before deciding it is wrong.

**`checklevels`** — which tiers run. `1`, `2` and `4` are right for almost every
network. Levels `5` and `6` also need `database-checks: true`; together they hold
over a million addresses, which is real memory for a small gain unless you are
being targeted specifically.

API keys are optional. A `vpnapi.io` key is the single biggest accuracy gain if
you want one.

---

## Architecture

```
core/          knows nothing about either proxy
  platform/    the three things a proxy has to provide
  antivpn/     scoring, caches, ASN, GeoIP, IP binding
  managers/    config, messages, logging
bungee/        ~290 lines of translation
velocity/      ~330 lines of translation
```

The anti-VPN logic never mentions BungeeCord or Velocity. That is what lets one
jar serve both without the two halves drifting apart: there is only one
implementation to test, and one to fix.

---

## Files it creates

```
plugins/LXVPN/
  config.yml
  messages.yml
  blacklist.json          addresses that scored over the threshold
  ip-restrictions.json    account bindings
  GeoLite2-ASN.mmdb       downloaded on first start
  GeoLite2-Country.mmdb   only when country filtering is on
  logs/lxvpn.log
```

Both JSON files are written to a temporary file and moved into place. A crash
mid-write would otherwise leave a truncated allow list, which locks every
restricted account out on the next start.

---

## Building

```bash
mvn clean package
# target/LXVPN-1.0-BETA.jar
```

Java 17+ to build and run. Java 21 to get virtual threads.

Everything shaded is relocated — SnakeYAML, Jackson, MaxMind, org.json. One jar
loads next to arbitrary other plugins on two different proxies, and an
unrelocated SnakeYAML is the classic way to break a server that was working fine
before this was installed.

---

## Upgrading from 0.3.3-APLHA

The configuration format changed and the old file will not carry over. See
[CHANGELOG.md](CHANGELOG.md) for the full key-by-key migration table.

The short version: delete `config.yml` and let a fresh one be written, then
change the permission node from `lxvpn.command` to `lxvpn.admin`.

---

## Contributing

This is a beta, so reports are the point. BungeeCord reports are especially
wanted — that half is the untested one.

Bug reports with console output get fixed. "It doesn't work" gets a request for
console output.

If you are reporting a false positive, include the output of
`/lxvpn check <address>` — it names the score, and that is the difference between
a five-minute fix and a guess.

---

## Licence

See [LICENSE](LICENSE).
