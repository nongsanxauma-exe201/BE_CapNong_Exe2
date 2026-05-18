package com.swd301.foodmarket.constant;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class BankBinConstants {


    /**
     * Key   = Tên ngân hàng
     * Value = Bank BIN
     */
    public static final Map<String, Long> BANK_BINS;

    static {
        Map<String, Long> map = new LinkedHashMap<>();

        // Big 4 Banks
        map.put("Vietcombank", 970436L);
        map.put("BIDV", 970418L);
        map.put("VietinBank", 970415L);
        map.put("Agribank", 970405L);

        // Private Banks
        map.put("Techcombank", 970407L);
        map.put("ACB", 970416L);
        map.put("Sacombank", 970403L);
        map.put("MB Bank", 970422L);
        map.put("VPBank", 970432L);
        map.put("TPBank", 970423L);
        map.put("SHB", 970443L);
        map.put("HDBank", 970437L);
        map.put("VIB", 970441L);
        map.put("OCB", 970448L);
        map.put("SeABank", 970440L);
        map.put("Eximbank", 970431L);
        map.put("MSB", 970426L);
        map.put("Nam A Bank", 970428L);
        map.put("Bac A Bank", 970409L);
        map.put("ABBank", 970425L);
        map.put("PVcomBank", 970412L);
        map.put("LPBank", 970449L);
        map.put("KienlongBank", 970452L);
        map.put("VietBank", 970433L);
        map.put("BaoViet Bank", 970438L);
        map.put("Saigonbank", 970400L);
        map.put("Woori Bank", 970457L);
        map.put("CIMB", 422589L);

        BANK_BINS = Collections.unmodifiableMap(map);
    }

    /**
     * Lấy BIN theo tên ngân hàng.
     *
     * Ví dụ:
     * Long bin = BankBinConstants.getBankBin("Vietcombank");
     */
    public static Long getBankBin(String bankName) {
        return BANK_BINS.get(bankName);
    }

    /**
     * Lấy BIN theo tên ngân hàng (không phân biệt hoa thường).
     */
    public static Long getBankBinIgnoreCase(String bankName) {
        if (bankName == null || bankName.isBlank()) {
            return null;
        }

        return BANK_BINS.entrySet()
                .stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase(bankName.trim()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
