package com.minibloomberg.ui;

import com.minibloomberg.logic.LivePriceManager;
import com.minibloomberg.data.Stock;
import com.minibloomberg.MainWindow;

public class SearchController {
    private final FadeTransitionPanel centerContainer;
    private final TickerDetailPanel[] panelHolder;
    private final LivePriceManager livePriceManager;
    private final MainWindow mainWindow;

    public SearchController(MainWindow mainWindow, FadeTransitionPanel centerContainer,
                            TickerDetailPanel[] panelHolder, LivePriceManager manager) {
        this.mainWindow = mainWindow;
        this.centerContainer = centerContainer;
        this.panelHolder = panelHolder;
        this.livePriceManager = manager;
    }

    public void search(String ticker) {
        ticker = ticker.toUpperCase().trim();
        if (ticker.isEmpty()) return;

        String currentTicker = panelHolder[0] != null
                ? panelHolder[0].getCurrentTicker() : null;

        if (ticker.equalsIgnoreCase(currentTicker)) {
            ToastManager.showToast(mainWindow, "Already viewing <b>" + ticker + "</b>.", ToastType.INFO);
            return;
        }

        Stock snapshot = com.minibloomberg.logic.StockDataFetcher.fetchStockSnapshot(ticker);

        if (snapshot == null) {
            ToastManager.showToast(mainWindow,
                    "<b>\"" + ticker + "\" is not a valid stock ticker.</b><br>Please check the symbol and try again.",
                    ToastType.ERROR);
        } else {
            TickerDetailPanel newPanel = new TickerDetailPanel(ticker, livePriceManager);
            panelHolder[0] = newPanel;
            centerContainer.removeAll();
            centerContainer.showWithFade(newPanel);
            centerContainer.revalidate();
            centerContainer.repaint();
        }
    }
}
