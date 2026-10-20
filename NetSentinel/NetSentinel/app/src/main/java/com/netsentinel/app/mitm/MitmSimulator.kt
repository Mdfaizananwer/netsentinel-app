package com.netsentinel.app.mitm

/**
 * Purely local, purely illustrative data for the "MITM Learning Lab" screens.
 *
 * IMPORTANT: Nothing in this file (or anywhere else in the app) performs ARP spoofing, DNS
 * spoofing, traffic redirection, or credential interception against real devices. This models
 * a fictional three-node topology (victim / attacker / gateway) entirely in memory so the UI can
 * visualize *how* the attack classes work conceptually, using canned example packets that are
 * never sent on any real network.
 */
object MitmSimulator {

    data class SimNode(val name: String, val ip: String, val mac: String, val role: String)
    data class SimPacket(val step: Int, val from: SimNode, val to: SimNode, val summary: String, val note: String)

    val victim = SimNode("Victim phone", "192.168.1.50", "AA:BB:CC:00:01:11", "Victim")
    val attacker = SimNode("Attacker laptop", "192.168.1.66", "AA:BB:CC:00:02:22", "Attacker (simulated)")
    val gateway = SimNode("Router / Gateway", "192.168.1.1", "AA:BB:CC:00:00:01", "Legitimate gateway")

    /** Educational step-through of classic ARP spoofing — illustrative only, never executed. */
    fun arpSpoofScenario(): List<SimPacket> = listOf(
        SimPacket(1, gateway, victim, "Legitimate: Gateway's real ARP entry for 192.168.1.1 → AA:BB:CC:00:00:01",
            "Normal network state before any attack."),
        SimPacket(2, attacker, victim, "Forged ARP reply: \"192.168.1.1 is at AA:BB:CC:00:02:22\"",
            "In a real attack this unsolicited/forged reply overwrites the victim's ARP cache. NetSentinel never sends this."),
        SimPacket(3, victim, attacker, "Victim's traffic for the gateway now (in the attack scenario) goes to the attacker's MAC first",
            "This is the mechanism that enables interception — it relies on ARP having no authentication."),
        SimPacket(4, attacker, gateway, "Attacker forwards traffic to the real gateway (if relaying)",
            "This is what makes it 'transparent' to the victim if the attacker chooses to relay traffic."),
    )

    /** Educational step-through of DNS spoofing — illustrative only, never executed. */
    fun dnsSpoofScenario(): List<SimPacket> = listOf(
        SimPacket(1, victim, gateway, "Legitimate DNS query: example.com?", "Normal resolution path."),
        SimPacket(2, attacker, victim, "Forged DNS response: example.com = <attacker-controlled IP>",
            "A spoofed/faster response can beat the real DNS server's reply. NetSentinel never sends this."),
        SimPacket(3, victim, attacker, "Victim unknowingly connects to the attacker's IP, believing it's example.com",
            "This is why DNSSEC / DoH / DoT and HTTPS certificate validation matter."),
    )

    data class DefenseTip(val title: String, val body: String)

    val defenseTips = listOf(
        DefenseTip("Certificate validation matters",
            "HTTPS binds a domain to a certificate chain rooted in a trusted CA. A MITM without a trusted certificate for that domain cannot transparently decrypt or tamper with the connection without the browser/app raising a certificate error."),
        DefenseTip("Watch for gateway/ARP changes",
            "A legitimate gateway's IP-to-MAC mapping should be stable on a home network. A sudden, unexplained change (which this app's Defensive Monitor logs) can indicate ARP spoofing — though it can also be innocent (router replaced, DHCP lease changes)."),
        DefenseTip("DNS metadata can reveal anomalies",
            "Unexpected DNS servers, or queries resolving to addresses outside expected ranges, are worth investigating — the Packet Capture Lab's DNS filter shows exactly this for your own device's traffic."),
        DefenseTip("Client isolation and VPNs limit visibility — both ways",
            "Properly configured client (AP) isolation stops devices on the same Wi-Fi from reaching each other directly, which defeats this class of attack but also means NetSentinel's own scanner won't see isolated devices either."),
        DefenseTip("Prefer DoH/DoT and WPA3",
            "DNS-over-HTTPS/TLS resists on-path DNS spoofing, and WPA3 (vs open or WEP/WPA networks) raises the bar for an attacker to even get on-path in the first place."),
    )
}
