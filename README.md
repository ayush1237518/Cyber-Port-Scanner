# 🛡️ Network Port Scanner

A professional, multithreaded **TCP port scanner** with a modern JavaFX desktop GUI, built in Java 21. Designed as a cybersecurity portfolio project demonstrating clean architecture, concurrency, and secure coding practices — **not** a beginner's socket-loop script.

> ⚠️ **Ethical use only.** Only scan hosts and networks you own or have explicit written permission to test. Unauthorized port scanning may violate laws such as the U.S. Computer Fraud and Abuse Act or equivalent legislation in your jurisdiction.

---

## 🌐 Landing Page

A dark, hacker-styled static landing page for this project lives in [`landing-page/`](landing-page/) — hero, live animated terminal, feature grid, screenshot showcase, architecture diagram, tech stack, and quickstart docs. It's a plain HTML/CSS/JS site with no build step, ready to deploy to **GitHub Pages** or **Netlify**. See [`landing-page/DEPLOY.md`](landing-page/DEPLOY.md) for one-click deployment instructions.

---

## 📖 Project Overview

Network Port Scanner lets you scan a single IPv4 address or hostname across a configurable port range, using a pool of concurrent worker threads for high-speed results. It classifies each port as **Open**, **Closed**, or **Filtered**, identifies likely services by port number, and — for open ports — attempts safe banner grabbing to reveal service version information. Results can be exported to **CSV**, **JSON**, or **TXT** for reporting.

The project is organized around SOLID principles with a clear separation between scanning logic, service detection, export strategies, input validation, and the UI layer, making it easy to extend or test in isolation.

---

## ✨ Features

- Scan any IPv4 address or hostname with DNS resolution
- Configurable start port, end port, timeout, and thread count
- Full input validation with friendly error messages
- Multithreaded scanning via `ExecutorService` for high performance
- Port classification: **Open / Closed / Filtered / Error**
- Service name identification for 25+ well-known ports (HTTP, HTTPS, SSH, FTP, SMTP, DNS, MySQL, PostgreSQL, Telnet, POP3, IMAP, RDP, SMB, MongoDB, Redis, and more)
- Safe, non-crashing banner grabbing on open ports
- Live progress tracking: percentage complete, ports scanned/remaining, elapsed time, and estimated time remaining
- Full scan summary statistics on completion
- Export reports to **CSV**, **JSON**, and **TXT** — no database required
- Modern dark cybersecurity-themed JavaFX GUI with blue neon accents
- Live-filterable, sortable results table
- Real-time activity log panel
- Keyboard shortcuts, tooltips, and animated progress bar
- Safe scan cancellation mid-run

---

## 🖼️ Screenshots

> _Add your own screenshots here after running the app._

```
docs/screenshots/main-window.png
docs/screenshots/scan-in-progress.png
docs/screenshots/export-dialog.png
```

---

## 🛠️ Technologies Used

| Category         | Technology                          |
|-------------------|-------------------------------------|
| Language          | Java 21 (LTS)                       |
| Build Tool        | Apache Maven                        |
| GUI Framework     | JavaFX 21                           |
| Concurrency       | `java.util.concurrent` (ExecutorService, Future) |
| Networking        | `java.net` (Socket, InetAddress)    |
| Testing           | JUnit 5                             |
| Report Formats    | CSV, JSON, TXT (file-based, no DB)  |

---

## 📦 Installation

### Prerequisites
- **JDK 21** or later ([Adoptium Temurin](https://adoptium.net/) recommended)
- **Apache Maven 3.9+**

### Clone the repository
```bash
git clone https://github.com/<your-username>/network-port-scanner.git
cd network-port-scanner
```

### Build the project
```bash
mvn clean package
```

This produces a runnable fat-jar at `target/network-port-scanner-1.0.0.jar`.

---

## ▶️ How to Run

### Option 1 — Maven JavaFX plugin (recommended for development)
```bash
mvn javafx:run
```

### Option 2 — Run the packaged jar directly
```bash
java -jar target/network-port-scanner-1.0.0.jar
```

> If you see a JavaFX runtime error when running the jar directly, ensure your JDK distribution bundles JavaFX, or run via `mvn javafx:run` instead.

---

## 🖥️ Usage

1. Enter a **target** — an IPv4 address (e.g. `192.168.1.1`) or hostname (e.g. `example.com`).
2. Set the **start port** and **end port** for the scan range.
3. Configure the **timeout** (in milliseconds) and **thread count** for performance tuning.
4. Optionally enable **banner grabbing** to attempt reading service banners.
5. Click **Start Scan** (or press `Ctrl+Enter`).
6. Watch live progress — percentage, ports scanned, elapsed/remaining time — in the sidebar.
7. Use the **search box** to filter results, or click column headers to sort.
8. Click **Export** to save a report as CSV, JSON, or TXT.
9. Use **Stop Scan** (`Esc`) to cancel safely at any time.

### ⌨️ Keyboard Shortcuts
| Shortcut       | Action           |
|----------------|------------------|
| `Ctrl + Enter` | Start scan       |
| `Esc`          | Stop scan        |
| `Ctrl + L`     | Clear results    |
| `Ctrl + F`     | Focus filter box |

---

## 📁 Folder Structure

```
network-port-scanner/
├── pom.xml
├── README.md
├── LICENSE
├── .gitignore
└── src/
    └── main/
        ├── java/com/portscanner/
        │   ├── ui/            # JavaFX controllers and application bootstrap
        │   │   ├── MainApp.java
        │   │   └── ScannerController.java
        │   ├── scanner/       # Core scanning engine
        │   │   ├── PortScanner.java
        │   │   ├── ScanTask.java
        │   │   └── ScanProgressListener.java
        │   ├── services/      # Service identification & banner grabbing
        │   │   ├── ServiceDetector.java
        │   │   └── BannerGrabber.java
        │   ├── model/         # Immutable domain models
        │   │   ├── ScanResult.java
        │   │   ├── ScanConfig.java
        │   │   ├── ScanStatistics.java
        │   │   └── PortStatus.java
        │   ├── exporter/       # Strategy-pattern report exporters
        │   │   ├── ReportExporter.java
        │   │   ├── ExporterFactory.java
        │   │   ├── CSVExporter.java
        │   │   ├── JSONExporter.java
        │   │   └── TXTExporter.java
        │   ├── validation/    # Input validation
        │   │   ├── InputValidator.java
        │   │   └── ValidationException.java
        │   ├── threading/      # ExecutorService management
        │   │   ├── ScanExecutorManager.java
        │   │   └── ScannerThreadFactory.java
        │   └── utils/          # Cross-cutting helpers
        │       ├── AppLogger.java
        │       └── TimerUtil.java
        └── resources/
            ├── fxml/main.fxml
            └── css/dark-theme.css
```

---

## 🏗️ Architecture & Design Principles

- **Single Responsibility** — each class has one clear job (e.g. `ScanTask` scans exactly one port; `BannerGrabber` only grabs banners).
- **Open/Closed** — new export formats can be added by implementing `ReportExporter` without modifying existing exporters.
- **Dependency Inversion** — the UI depends on the `ScanProgressListener` interface and `ReportExporter` abstraction, not concrete implementations.
- **Thread Safety** — scan results are collected via a `CopyOnWriteArrayList`/synchronized list, and all UI mutations are marshalled onto the JavaFX Application Thread via `Platform.runLater`.
- **Graceful Degradation** — banner grabbing, DNS resolution, and socket I/O failures are all caught and classified rather than allowed to crash the scan.

---

## 🔒 Security & Ethical Notes

- The scanner performs only **standard TCP connect scans** — no raw sockets, no packet spoofing, no exploitation payloads.
- No credentials, exploits, or attack payloads are included anywhere in this codebase.
- Always obtain authorization before scanning any network you do not own.

---

## 🚀 Future Improvements

- CIDR subnet scanning (scan an entire `/24` range, not just a single host)
- Active network discovery (ARP/ICMP host discovery before port scanning)
- Vulnerability scanning integration (e.g. CVE lookups against detected service banners)
- Nmap integration for hybrid scan/reporting workflows
- Scheduled/recurring scans with historical comparison
- User authentication for multi-user deployments
- Optional database support for long-term report storage
- Web-based report dashboard with charts and trend analysis

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
