package com.netsentinel.app.navigation

sealed class Destination(val route: String, val title: String) {
    object Dashboard : Destination("dashboard", "Dashboard")
    object WifiAnalyzer : Destination("wifi_analyzer", "Wi-Fi Analyzer")
    object Scanner : Destination("scanner", "Network Scanner")
    object Inventory : Destination("inventory", "Device Inventory")
    object DeviceDetail : Destination("inventory/{ip}", "Device Detail") {
        fun route(ip: String) = "inventory/$ip"
    }
    object Capture : Destination("capture", "Packet Capture Lab")
    object MitmLab : Destination("mitm_lab", "MITM Security Lab")
    object Reports : Destination("reports", "Security Reports")
    object Privacy : Destination("privacy", "Privacy & Data")
    object DeviceControl : Destination("device_control", "Device Control")
}
