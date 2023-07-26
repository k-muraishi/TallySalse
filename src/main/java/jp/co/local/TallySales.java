package jp.co.local;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jp.co.local.Service.ProductData;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.text.SimpleDateFormat;
import java.util.*;

public class TallySales {
    private static final SimpleDateFormat DATE_FORMAT_IN1 = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_FORMAT_IN2 = new SimpleDateFormat("yyyy年MM月dd日");
    private static final SimpleDateFormat DATE_FORMAT_OUT = new SimpleDateFormat("yyyy/MM/dd");

    public static void main(String[] args) {
        List<ProductData> productList = parseCSVData("taegetCsv/sales.csv");

        // 1. 商品の種類と単価をデータから推測
        Map<String, Double> productPrices = getProductPrices(productList);
        System.out.println("1. 商品の種類と単価:");
        for (Map.Entry<String, Double> entry : productPrices.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + "円");
        }

        // 2. 商品の種類ごとの売上金額を集計
        Map<String, Double> productSalesByType = getProductSalesByType(productList, productPrices);
        System.out.println("\n2. 商品の種類ごとの売上金額:");
        for (Map.Entry<String, Double> entry : productSalesByType.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue() + "円");
        }

        // 3. 年月別に集計し、東京の売上が一番大きかった月の最も売り上げた商品を答える
        String bestProductByMonth = getBestMonthInTokyo(productList);
        System.out.println("\n3. 東京の売上が一番大きかった月: " + bestProductByMonth);

        // 4. Excelに集計結果を出力
        writeDataToExcel(productPrices, productSalesByType, bestProductByMonth);
        System.out.println("\n集計結果をExcelファイルに出力しました。");
    }

    private static List<ProductData> parseCSVData(String csvFilePath) {
        List<ProductData> productList = new ArrayList<>();
        List<CSVRecord> errorRecords = new ArrayList<>();
        try (Reader reader = new FileReader(csvFilePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            for (CSVRecord record : csvParser) {
                // カラムの数がヘッダーのカラム数より多い場合はエラーとして保存
                if (record.size() != csvParser.getHeaderMap().size()) {
                    errorRecords.add(record);
                    continue;
               // データに空白文字が存在する場合
                }else if (hasEmptyOrNullValues(record)) {
                    errorRecords.add(record);
                    continue;
                }

                int id = Integer.parseInt(record.get("id"));
                System.out.println(id);
                String productName = record.get("product_name");
                Date salesDate = parseDate(record.get("sales_date"));
                int quantity = Integer.parseInt(record.get("quantity"));
                double sales = Double.parseDouble(record.get("sales"));
                String store = record.get("store");
                productList.add(new ProductData(id, productName, salesDate, quantity, sales, store));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // エラーデータをerrors/errors.csvに出力
        if (!errorRecords.isEmpty()) {
            try (Writer writer = new FileWriter("errors/errors.csv");
                 CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(String.valueOf(errorRecords.get(0))))) {
                for (CSVRecord record : errorRecords) {
                    csvPrinter.printRecord(record);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return productList;
    }

    private static Date parseDate(String dateString) {
        try {
            if (dateString.contains("-")) {
                return DATE_FORMAT_IN1.parse(dateString);
            } else if (dateString.contains("年") && dateString.contains("月")) {
                return DATE_FORMAT_IN2.parse(dateString);
            } else {
                return DATE_FORMAT_OUT.parse(dateString); // デフォルトはyyyy/MM/dd形式としてパース
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean hasEmptyOrNullValues(CSVRecord record) {
        for (String value : record) {
            if (value == null || value.trim().isEmpty() || value.equals("")) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Double> getProductPrices(List<ProductData> productList) {
        Map<String, Double> productPrices = new HashMap<>();
        for (ProductData product : productList) {
            if (!productPrices.containsKey(product.getProductName())) {
                double price = product.getSales() / product.getQuantity();
                productPrices.put(product.getProductName(), price);
            }
        }
        return productPrices;
    }

    private static Map<String, Double> getProductSalesByType(List<ProductData> productList,
                                                             Map<String, Double> productPrices) {
        Map<String, Double> productSalesByType = new HashMap<>();
        for (ProductData product : productList) {
            String productName = product.getProductName();
            double sales = product.getSales();
            productSalesByType.put(productName, productSalesByType.getOrDefault(productName, 0.0) + sales);
        }
        return productSalesByType;
    }

    private static String getBestMonthInTokyo(List<ProductData> productList) {
        Map<String, Double> salesByMonth = new HashMap<>();
        Map<String, String> bestProductByMonth = new HashMap<>();

        for (ProductData product : productList) {
            if ("東京".equals(product.getStore())) {
                String monthYear = DATE_FORMAT_OUT.format(product.getSalesDate());
                double sales = salesByMonth.getOrDefault(monthYear, 0.0) + product.getSales();
                salesByMonth.put(monthYear, sales);

                // 最も売り上げた商品を更新
                String currentBestProduct = bestProductByMonth.getOrDefault(monthYear, "");
                if (currentBestProduct.isEmpty() || product.getSales() > salesByMonth.get(monthYear)) {
                    bestProductByMonth.put(monthYear, product.getProductName());
                }
            }
        }

        double maxSales = 0;
        String bestMonth = null;
        for (Map.Entry<String, Double> entry : salesByMonth.entrySet()) {
            if (entry.getValue() > maxSales) {
                maxSales = entry.getValue();
                bestMonth = entry.getKey();
            }
        }
        String bestProduct = bestProductByMonth.get(bestMonth);
        System.out.println("最も売り上げた商品: " + bestProduct);
        return bestProductByMonth.get(bestProduct);
    }

    private static String getBestProductInBestMonth(List<ProductData> productList, String bestMonth) {
        Map<String, Double> productSalesInBestMonth = new HashMap<>();
        for (ProductData product : productList) {
            String monthYear = DATE_FORMAT_OUT.format(product.getSalesDate());
            if (monthYear.equals(bestMonth)) {
                String productName = product.getProductName();
                double sales = product.getSales();
                productSalesInBestMonth.put(productName, sales);
            }
        }

        String bestProduct = null;
        double maxSalesInBestMonth = 0;
        for (Map.Entry<String, Double> entry : productSalesInBestMonth.entrySet()) {
            if (entry.getValue() > maxSalesInBestMonth) {
                maxSalesInBestMonth = entry.getValue();
                bestProduct = entry.getKey();
            }
        }
        return bestProduct;
    }

    private static void writeDataToExcel(Map<String, Double> productPrices,
                                         Map<String, Double> productSalesByType,
                                         String bestProductByMonth) {
        try (Workbook workbook = new XSSFWorkbook()) {
            writeSheet(workbook, "商品の種類と単価", productPrices);
            writeSheet(workbook, "商品の種類ごとの売上金額", productSalesByType);
            writeSingleCellSheet(workbook, "東京の売上が一番大きかった月の商品", bestProductByMonth);
            File outputDir = new File("result");
            outputDir.mkdirs();
            FileOutputStream fileOut = new FileOutputStream("result/result.xlsx");
            workbook.write(fileOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeSheet(Workbook workbook, String sheetName, Map<String, Double> data) {
        Sheet sheet = workbook.createSheet(sheetName);
        int rowIdx = 0;
        for (Map.Entry<String, Double> entry : data.entrySet()) {
            Row row = sheet.createRow(rowIdx++);
            Cell cell1 = row.createCell(0);
            cell1.setCellValue(entry.getKey());
            Cell cell2 = row.createCell(1);
            cell2.setCellValue(entry.getValue());
        }
    }

    private static void writeSingleCellSheet(Workbook workbook, String sheetName, String cellValue) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row row = sheet.createRow(0);
        Cell cell = row.createCell(0);
        cell.setCellValue(cellValue);
    }
}