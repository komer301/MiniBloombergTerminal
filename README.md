# Mini Bloomberg Terminal

A fully-featured Java-based desktop stock tracker inspired by Bloomberg Terminal, built using Swing. This project provides real-time stock price updates, interactive charts, trending news, and a live trade tape — all within a polished UI.

---

## Getting Started

**[Design Document](https://docs.google.com/document/d/1rgUpsVnDmOD19XZQCELgm1F_iguyVkT_zNz60yAUrQM/edit?usp=sharing)** – Learn more about our architecture, features, and design choices.

---

## Features

- **Interactive Line Charts**  
  Historical price visualization for searched stocks, dynamically padded for low-variation tickers and adjusted for varying time ranges.

- **Live Financial News Feed**  
  Auto-fetches the latest news for tracked companies using Finnhub.

- **Real-Time Price Updates**  
  Live ticker prices and percent changes streamed via WebSocket from Finnhub, displayed in the watchlist and detail view.

- **Trade Tape Simulation**  
  In off-market hours, simulates top gainer/loser/active stocks using Alpha Vantage fallback APIs with animated trade display.

- **Toast Notifications**  
  Non-intrusive, styled toast messages for feedback (e.g., invalid ticker, duplicate searches, etc.).

---

## Key Components Breakdown

### `MainWindow.java`
- Initializes layout and UI panels.
- Manages interactions between `SearchController`, `WatchlistPanel`, `NewsPanel`, and `TradeTapePanel`.

### `SearchController.java`
- Manages all search behavior.
- Interfaces with `StockDataFetcher` to validate ticker.
- Displays error toasts or loads `TickerDetailPanel` if valid.

### `ChartPanel.java`
- Dynamically draws historical price charts.
- Modular rendering logic (Y-axis, X-axis, crosshair) for maintainability.
- Adaptive padding and scaling for low-volatility data.

### `LivePriceManager.java`
- Establishes WebSocket to stream live prices via Finnhub.
- Updates watchlist and ticker details in real time.

### `NewsFetcher.java` + `NewsPanel.java`
- Fetches latest financial news.
- Displays headline, source, summary, and timestamp.

### `TradeTapeManager.java` + `TradeTapePanel.java`
- Streams real-time trade items while market is open.
- Switches to simulated "top tickers" trade feed during after-hours using Alpha Vantage.

### `StockDataFetcher.java`
- Fetches:
    - Stock snapshot (quote + profile)
    - Historical data (for charts)
    - Fuzzy ticker suggestions (via Alpha Vantage `SYMBOL_SEARCH`)

---

## API Keys Required

You must create a `.env` file in the project root with the following keys:

- ALPHA_API_KEY=your_alpha_vantage_key_here
- FINNHUB_API_KEY=your_finnhub_key_here 


> These are used for fetching real-time stock quotes, company metadata, news, and historical data.

---

## Dependencies

- [java-websocket](https://github.com/TooTallNate/Java-WebSocket)
- [org.json](https://github.com/stleary/JSON-java)
- [dotenv-java](https://github.com/cdimascio/dotenv-java) – For loading API keys from `.env`
- [Alpha Vantage API](https://www.alphavantage.co/documentation/) – Stock, symbol search, historical data
- [Finnhub API](https://finnhub.io/docs/api) – Quotes, news, real-time streaming

---

## Getting Started

1. Clone the repo  
   `git clone https://github.com/your-username/minibloomberg-terminal.git`

2. Add your `.env` file  
   See API keys section above.

3. Run `MainWindow.java` from your IDE  
   The app should launch immediately!

---

## Future Ideas

- Add stock alerts and price triggers.
- Integrate more APIs for company fundamentals.
- Support multi-ticker chart overlays.
- Export charts as PNG.
- Dark/light theme toggle.

---

## Author (s)

Developed by Khalid Omer & Prince Tsiquaye, 2025.  
Inspired by Bloomberg, built for learning, testing, and tinkering.

---
