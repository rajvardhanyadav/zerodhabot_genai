package com.tradingbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingbot.config.KiteConfig;
import com.tradingbot.dto.AccountInfo;
import com.tradingbot.dto.NfoInstrument;
import com.tradingbot.dto.PaperTrade;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class KiteService {
    private static final Logger logger = LoggerFactory.getLogger(KiteService.class);

    @Autowired
    private KiteConfig kiteConfig;

    /**
     * -- GETTER --
     *  Check if paper trading is enabled
     */
    @Getter
    @Value("${trading.paper.enabled:false}")
    private boolean paperTradingEnabled;

    @Autowired
    KiteTickerService kiteTickerService;

    private String accessToken;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    // Paper trading storage
    private final Map<String, PaperTrade> paperTrades = new ConcurrentHashMap<>();
    private int paperOrderIdCounter = 1000;

    public String getLoginUrl(String requestToken) {
        return kiteConfig.getLoginUrl() + "?api_key=" + kiteConfig.getKey() + "&v=3";
    }

    public boolean generateAccessToken(String requestToken) {
        try {
            HttpPost post = new HttpPost(kiteConfig.getBaseUrl() + "/session/token");
            List<NameValuePair> params = Arrays.asList(
                    new BasicNameValuePair("api_key", kiteConfig.getKey()),
                    new BasicNameValuePair("request_token", requestToken),
                    new BasicNameValuePair("checksum", generateChecksum(requestToken))
            );
            post.setEntity(new UrlEncodedFormEntity(params));

            String response = httpClient.execute(post, httpResponse -> {
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });

            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("data")) {
                this.accessToken = jsonResponse.get("data").get("access_token").asText();
                logger.info("Access token generated successfully : " + this.accessToken);
                kiteTickerService.connect(kiteConfig.getKey(), this.accessToken);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error generating access token", e);
        }
        return false;
    }

    public AccountInfo getAccountInfo() {
        logger.info("Fetching account info");
        try {
            HttpGet get = new HttpGet(kiteConfig.getBaseUrl() + "/user/margins");
            get.setHeader("Authorization", "token " + kiteConfig.getKey() + ":" + accessToken);

            String response = httpClient.execute(get, httpResponse -> {
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });

            log.info(response);
            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("data")) {
                JsonNode data = jsonResponse.get("data").get("equity");
                AccountInfo accountInfo = new AccountInfo();
                accountInfo.setAvailableMargin(new BigDecimal(data.get("available").get("live_balance").asText()));
                accountInfo.setUtilizedMargin(new BigDecimal(data.get("utilised").get("debits").asText()));
                accountInfo.setTotalMargin(accountInfo.getAvailableMargin().add(accountInfo.getUtilizedMargin()));
                return accountInfo;
            }
        } catch (Exception e) {
            logger.error("Error fetching account info", e);
        }
        return null;
    }

    /**
     * Main order placement method that routes to real or paper trading based on configuration
     */
    public String placeOrder(String symbol, String transactionType, int quantity, String orderType, BigDecimal price) {
        if (paperTradingEnabled) {
            return placePaperOrder(symbol, transactionType, quantity, orderType, price);
        } else {
            return placeRealOrder(symbol, transactionType, quantity, orderType, price);
        }
    }

    /**
     * Real order placement method
     */
    private String placeRealOrder(String symbol, String transactionType, int quantity, String orderType, BigDecimal price) {
        log.info("placeRealOrder() - Symbol: {}, Type: {}, Qty: {}", symbol, transactionType, quantity);
        try {
            HttpPost post = new HttpPost(kiteConfig.getBaseUrl() + "/orders/regular");
            post.setHeader("Authorization", "token " + kiteConfig.getKey() + ":" + accessToken);

            List<NameValuePair> params = Arrays.asList(
                    new BasicNameValuePair("tradingsymbol", symbol),
                    new BasicNameValuePair("exchange", "NFO"),
                    new BasicNameValuePair("transaction_type", transactionType),
                    new BasicNameValuePair("order_type", orderType),
                    new BasicNameValuePair("quantity", String.valueOf(quantity)),
                    new BasicNameValuePair("price", price.toString()),
                    new BasicNameValuePair("product", "MIS"),
                    new BasicNameValuePair("validity", "DAY")
            );
            post.setEntity(new UrlEncodedFormEntity(params));

            String response = httpClient.execute(post, httpResponse -> {
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });

            log.info("Real order response : " + response);

            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("data")) {
                String orderId = jsonResponse.get("data").get("order_id").asText();
                log.info("Real order placed successfully. Order ID: {}", orderId);
                return orderId;
            }
        } catch (Exception e) {
            logger.error("Error placing real order", e);
        }
        return null;
    }

    private String placePaperOrder(String symbol, String transactionType, int quantity, String orderType, BigDecimal price) {
        log.info("placePaperOrder() - Symbol: {}, Type: {}, Qty: {}", symbol, transactionType, quantity);
        try {
            String paperOrderId = "PAPER_" + (paperOrderIdCounter++);

            BigDecimal marketPrice = getLastPrice(symbol);
            log.info("LTP : "+marketPrice);
            if (marketPrice.equals(BigDecimal.ZERO)) {
                log.warn("Could not fetch market price for paper order simulation: {}", symbol);
                marketPrice = price;
            }

            PaperTrade paperTrade = new PaperTrade();
            paperTrade.setOrderId(paperOrderId);
            paperTrade.setSymbol(symbol);
            paperTrade.setTransactionType(PaperTrade.TransactionType.valueOf(transactionType.toUpperCase())); // Convert String to Enum
            paperTrade.setQuantity(quantity);
            paperTrade.setOrderType(PaperTrade.OrderType.valueOf(orderType.toUpperCase())); // Convert String to Enum
            paperTrade.setOrderPrice(price);
            paperTrade.setMarketPrice(marketPrice);
            paperTrade.setOrderTime(LocalDateTime.now());
            paperTrade.setStatus(PaperTrade.Status.COMPLETE); // Use Enum directly

            BigDecimal executionPrice = PaperTrade.OrderType.MARKET.equals(paperTrade.getOrderType()) ? marketPrice : price;
            paperTrade.setExecutionPrice(executionPrice);
            paperTrade.setPnl(BigDecimal.ZERO);

            paperTrades.put(paperOrderId, paperTrade);

            log.info("Paper order placed successfully. Order ID: {}, Symbol: {}, Price: {}, Market Price: {}",
                    paperOrderId, symbol, executionPrice, marketPrice);

            return paperOrderId;

        } catch (Exception e) {
            logger.error("Error placing paper order", e);
            return null;
        }
    }

    private NfoInstrument getInstrument(String nfoData, int atmStrike, String instrumentType, LocalDate selectedExpiryDate) {
        List<NfoInstrument> nfoInstruments = new ArrayList<>();
        for (Map.Entry<String, LocalDate> entry : bankNiftyExpiries.entrySet()) {
            String[] fields1 = entry.getKey().split(",");
            if (fields1.length > 10 && fields1[2].startsWith("BANKNIFTY") &&
                    fields1[9].equals(instrumentType) && LocalDate.parse(fields1[5]).isEqual(selectedExpiryDate) &&
                    Integer.parseInt(fields1[6]) == atmStrike) {
                NfoInstrument nfoInstrument = new NfoInstrument();
                nfoInstrument.setInstrumentToken(fields1[0]); // Updated setter
                nfoInstrument.setExchangeToken(fields1[1]); // Updated setter
                nfoInstrument.setTradingSymbol(fields1[2]); // Updated setter
                nfoInstrument.setName(fields1[3]);
                nfoInstrument.setLastPrice(fields1[4]); // Updated setter
                nfoInstrument.setExpiry(fields1[5]);
                nfoInstrument.setStrike(fields1[6]);
                nfoInstrument.setTickSize(fields1[7]); // Updated setter
                nfoInstrument.setLotSize(fields1[8]); // Updated setter
                nfoInstrument.setInstrumentType(fields1[9]); // Updated setter
                nfoInstrument.setSegment(fields1[10]);
                nfoInstrument.setExchange(fields1[11]);
                return nfoInstrument;
            }
        }
        return null;
    }

    /**
     * Get paper trade details
     */
    public PaperTrade getPaperTrade(String orderId) {
        return paperTrades.get(orderId);
    }

    /**
     * Get all paper trades
     */
    public Map<String, PaperTrade> getAllPaperTrades() {
        return new HashMap<>(paperTrades);
    }

    /**
     * Calculate total P&L for all paper trades
     */
    public BigDecimal getTotalPaperPnL() {
        return paperTrades.values().stream()
                .map(PaperTrade::getPnl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Update P&L for paper trades based on current market prices
     */
    public void updatePaperTradesPnL() {
        log.info("Updating P&L for {} paper trades", paperTrades.size());

        for (PaperTrade trade : paperTrades.values()) {
            BigDecimal currentPrice = getLastPrice(trade.getSymbol());
            if (!currentPrice.equals(BigDecimal.ZERO)) {
                BigDecimal pnl = calculatePnL(trade, currentPrice);
                trade.setPnl(pnl);
                trade.setCurrentPrice(currentPrice);
                trade.setLastUpdated(LocalDateTime.now());
            }
        }
    }

    /**
     * Calculate P&L for a paper trade
     */
    private BigDecimal calculatePnL(PaperTrade trade, BigDecimal currentPrice) {
        BigDecimal priceDiff = currentPrice.subtract(trade.getExecutionPrice());

        // For SELL orders, reverse the P&L calculation
        if ("SELL".equals(trade.getTransactionType())) {
            priceDiff = priceDiff.negate();
        }

        return priceDiff.multiply(new BigDecimal(trade.getQuantity()));
    }

    /**
     * Clear all paper trades (useful for testing)
     */
    public void clearPaperTrades() {
        paperTrades.clear();
        paperOrderIdCounter = 1000;
        log.info("All paper trades cleared");
    }

    public BigDecimal getLastPrice(String symbol) {
        //log.info("getLastPrice()");
        try {
            HttpGet get = new HttpGet(kiteConfig.getBaseUrl() + "/quote/ltp?i=NFO:" + symbol);
            get.setHeader("Authorization", "token " + kiteConfig.getKey() + ":" + accessToken);

            String response = httpClient.execute(get, httpResponse -> {
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });

            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("data")) {
                return new BigDecimal(jsonResponse.get("data").get("NFO:" + symbol).get("last_price").asText());
            }
        } catch (Exception e) {
            logger.error("Error fetching last price for " + symbol, e);
        }
        return BigDecimal.ZERO;
    }

    private String generateChecksum(String requestToken) {
        try {
            String data = kiteConfig.getKey() + requestToken + kiteConfig.getSecret();
            return org.apache.commons.codec.digest.DigestUtils.sha256Hex(data);
        } catch (Exception e) {
            logger.error("Error generating checksum", e);
            return "";
        }
    }

    public boolean isAccessTokenValid() {
        logger.info("Checking access token validity");
        return accessToken != null && !accessToken.isEmpty();
    }

    public List<NfoInstrument> getATMStraddleSymbols(LocalDate selectedExpiryDate) {
        log.info("getATMStraddleSymbols()");
        try {
            // Get current BankNifty price
            BigDecimal currentPrice = getBankNiftyPrice();
            log.info("Banknifty currentPrice : " + currentPrice);
            if (currentPrice.equals(BigDecimal.ZERO)) {
                logger.error("Failed to fetch BankNifty price");
                return Collections.emptyList();
            }

            // Calculate ATM strike
            int atmStrike = calculateATMStrike(currentPrice);

            // Get current expiry from NSE data
            String nfoData = getCurrentBankNiftyExpiry();

//            log.info("Banknifty current expiry : " + nfoData);
            if (nfoData == null) {
                logger.error("Failed to fetch current expiry");
                return Collections.emptyList();
            }

            // Build symbols
            List<NfoInstrument> symbols = new ArrayList<>();
//            String ceSymbol = "BANKNIFTY" + nfoData + atmStrike + "CE";
            NfoInstrument nfoInstrumentCE = getInstrument(nfoData, atmStrike, "CE",selectedExpiryDate);
//            String peSymbol = "BANKNIFTY" + nfoData + atmStrike + "PE";
            NfoInstrument nfoInstrumentPE = getInstrument(nfoData, atmStrike, "PE",selectedExpiryDate);
            assert nfoInstrumentCE != null;
            log.info("ceSymbol : " + nfoInstrumentCE.getTradingSymbol());
            assert nfoInstrumentPE != null;
            log.info("peSymbol : " + nfoInstrumentPE.getTradingSymbol());

            symbols.add(nfoInstrumentCE);
            symbols.add(nfoInstrumentPE);

            logger.info("Generated ATM straddle symbols: CE={}, PE={} for strike={}",
                    nfoInstrumentCE.getTradingSymbol(), nfoInstrumentPE.getTradingSymbol(), atmStrike);

            return symbols;

        } catch (Exception e) {
            logger.error("Error generating ATM straddle symbols", e);
            return Collections.emptyList();
        }
    }

    public BigDecimal getBankNiftyPrice() {
        try {
            HttpGet get = new HttpGet(kiteConfig.getBaseUrl() + "/quote/ltp?i=NSE:NIFTY%20BANK");
            get.setHeader("Authorization", "token " + kiteConfig.getKey() + ":" + accessToken);
            get.setHeader("X-Kite-Version", "3");

            String response = httpClient.execute(get, httpResponse -> {
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });

            JsonNode jsonResponse = objectMapper.readTree(response);
            if (jsonResponse.has("data")) {
                return new BigDecimal(jsonResponse.get("data").get("NSE:NIFTY BANK").get("last_price").asText());
            }
        } catch (Exception e) {
            logger.error("Error fetching BankNifty price", e);
        }
        return BigDecimal.ZERO;
    }

    private int calculateATMStrike(BigDecimal currentPrice) {
        log.info("calculateATMStrike()");
        // BankNifty strikes are in multiples of 100
        int strike = currentPrice.divide(new BigDecimal(100), 0, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal(100))
                .intValue();
        log.info("ATM strike price : " + strike);
        return strike;
    }

    private Map<String, LocalDate> bankNiftyExpiries = new HashMap<>();
    private LocalDateTime expiryDataLastUpdated = null;

    public String getCurrentBankNiftyExpiry() {
        log.info("getCurrentBankNiftyExpiry()");
        // Refresh expiry data if older than 1 hour or empty
        if (expiryDataLastUpdated == null ||
                expiryDataLastUpdated.isBefore(LocalDateTime.now().minusHours(1)) ||
                bankNiftyExpiries.isEmpty()) {

            refreshExpiryData();
        }

        // Find nearest expiry
        LocalDate today = LocalDate.now();
        return bankNiftyExpiries.entrySet().stream()
                .filter(entry -> !entry.getValue().isBefore(today))
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    private void refreshExpiryData() {
        log.info("refreshExpiryData()");
        try {
            HttpGet get = new HttpGet(kiteConfig.getBaseUrl() + "/instruments/NFO");
            get.setHeader("Authorization", "token " + kiteConfig.getKey() + ":" + accessToken);

            String response = httpClient.execute(get, httpResponse -> {
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });

            bankNiftyExpiries.clear();
            String[] lines = response.split("\n");

            for (String line : lines) {
                String[] fields = line.split(",");
                if (fields.length > 10 && fields[2].startsWith("BANKNIFTY") &&
                        (fields[9].equals("CE") || fields[9].equals("PE"))) {

                    LocalDate expiry = LocalDate.parse(fields[5]);
                    bankNiftyExpiries.put(line, expiry);
                }
            }

            expiryDataLastUpdated = LocalDateTime.now();
            logger.info("Refreshed expiry data. Found {} unique expiries", bankNiftyExpiries.size());

        } catch (Exception e) {
            logger.error("Error refreshing expiry data", e);
        }
    }

    public List<LocalDate> getUpcomingExpiryDates(int i) {
        Map<String, LocalDate> expiryDates = new HashMap<>();
        try {
            HttpGet get = new HttpGet(kiteConfig.getBaseUrl() + "/instruments/NFO");
            get.setHeader("Authorization", "token " + kiteConfig.getKey() + ":" + accessToken);

            String response = httpClient.execute(get, httpResponse -> {
                return new String(httpResponse.getEntity().getContent().readAllBytes());
            });
            String[] lines = response.split("\n");
            log.info("Total line : "+lines.length);
            for (String line : lines) {
                String[] fields = line.split(",");
                if (fields.length > 10 && fields[2].startsWith("BANKNIFTY")){
                    expiryDates.put(fields[5], LocalDate.parse(fields[5]));
                }
            }
        } catch (Exception e) {
            logger.error("Error refreshing expiry data", e);
        }
        List<LocalDate> upcomingExpiries = new ArrayList<>();
        for (Map.Entry<String, LocalDate> entry : expiryDates.entrySet()) {
            String key = entry.getKey();
            LocalDate expiryDate = entry.getValue();
            if (expiryDate.isAfter(LocalDate.now())) {
                    upcomingExpiries.add(expiryDate);
            }
        }
        log.info("upcomingExpiries() : " + upcomingExpiries.size());
        return upcomingExpiries;
    }
}