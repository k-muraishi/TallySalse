package jp.co.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PropertyResourceBundle;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class TallySales {

    private static final SimpleDateFormat DATE_FORMAT_IN1 = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat DATE_FORMAT_IN2 = new SimpleDateFormat("yyyy年MM月dd日");
    private static final SimpleDateFormat DATE_FORMAT_OUT = new SimpleDateFormat("yyyy/MM/dd");
    private static Map<CSVRecord, String> errorRecordsMap = new HashMap<>();
    private static Map<String, Integer> headerMap = new HashMap<>();

    /**
     * 売上情報集計処理
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        FileReader in = null;
        File qsFile = new File("resource/tallySales.properties");
        in = new FileReader(qsFile);
        PropertyResourceBundle resource = new PropertyResourceBundle(in);
        String salesCsvPath = resource.getString("tallysales.sales.csv.path");
        String errCsvPath = resource.getString("tallysales.err.csv.path");
        String resultExcelPath = resource.getString("tallysales.result.excel.path");

        try {
            List<String> headers = new ArrayList<>();
            // csvデータ読み込み
            List<ProductData> productList = parseCSVData(salesCsvPath, headers);

            // エラーレコード書き出し
            if (!errorRecordsMap.isEmpty()) {
                outErrCsv(headers, errCsvPath);
            }

            // 1. 商品の種類と単価をデータから推測
            Map<String, Double> productPrices = getProductPrices(productList);
            System.out.println("\n〇 商品の種類と単価:");
            for (Map.Entry<String, Double> entry : productPrices.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue() + "円");
            }

            // 2. 商品の種類ごとの売上金額を集計
            Map<String, Double> productSalesByType = getProductSalesByType(productList, productPrices);
            System.out.println("\n〇 商品の種類ごとの売上金額:");
            for (Map.Entry<String, Double> entry : productSalesByType.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue() + "円");
            }

            // 3. 年月別に集計し、東京の売上が一番大きかった月の最も売り上げた商品を答える
            String bestProduct = getBestMonthInTokyo(productList);
            System.out.println("\n〇 東京の売上が一番大きかった月の商品: " + bestProduct);

            // 4. Excelに集計結果を出力
            writeDataToExcel(productPrices, productSalesByType, bestProduct, resultExcelPath);
            System.out.println("\n集計結果をExcelファイルに出力しました。");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * csvデータの読み込み
     * @param csvFilePath csvのパス
     * @param headers ヘッダー情報を格納するリスト
     * @return 結果情報 csv情報のリスト
     */
    private static List<ProductData> parseCSVData(String csvFilePath, List<String> headers) {
        List<ProductData> productList = new ArrayList<>();
        List<CSVRecord> errorRecords = new ArrayList<>();
        int id;
        String productName;
        Date salesDate;
        int quantity;
        double sales;
        String store;

        try (Reader reader = new FileReader(csvFilePath);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
            headerMap = csvParser.getHeaderMap();
            for (String header : headerMap.keySet()) {
                headers.add(header);
            }

            for (CSVRecord record : csvParser) {
                // カラムの数がヘッダーのカラム数と合致していない場合はエラーとして保存
                if (record.size() != csvParser.getHeaderMap().size()) {
                    errorRecordsMap.put(record, "カラムの数がヘッダーのカラム数と合致しておりません");
                    errorRecords.add(record);
                    continue;
                    // データに空白文字が存在する場合
                } else if (hasEmptyOrNullValues(record)) {
                    errorRecordsMap.put(record, " データに空白文字が存在しております。");
                    errorRecords.add(record);
                    continue;
                }

                id = Integer.parseInt(record.get("id"));
                productName = record.get("product_name");
                salesDate = parseDate(record.get("sales_date"));
                quantity = Integer.parseInt(record.get("quantity"));
                sales = Double.parseDouble(record.get("sales"));
                store = record.get("store");
                productList.add(new ProductData(id, productName, salesDate, quantity, sales, store));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return productList;
    }

    /**
     * エラーデータの書き出し
     * @param headers   ヘッダー情報を格納するリスト
     * 	@param errCsvPath  ファイルパス
     */
    private static void outErrCsv(List<String> headers, String errCsvPath) {
        // エラーデータをerrors/errors.csvに出力
        if (!errorRecordsMap.isEmpty()) {
            List<String> headersList = new ArrayList<>(headers);
            headersList.add(0, "エラー原因");
            try (Writer writer = new FileWriter(errCsvPath);
                 CSVPrinter csvPrinter = new CSVPrinter(writer,
                         CSVFormat.DEFAULT.withHeader(headersList.toArray(new String[0])))) {
                for (Map.Entry<CSVRecord, String> entry : errorRecordsMap.entrySet()) {
                    CSVRecord record = entry.getKey();
                    String errorMessage = entry.getValue();

                    // レコードの各値とエラーメッセージをCSVに出力
                    csvPrinter.print(errorMessage);
                    for (String value : record) {
                        csvPrinter.print(value);
                    }
                    csvPrinter.println();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 売上日のパース
     * @param dateString   売上日
     * @return 結果情報
     */
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

    /**
     * 空白データの判定
     * @param record   各レコード
     * @return 結果情報 boolean
     */
    private static boolean hasEmptyOrNullValues(CSVRecord record) {
        for (String value : record) {
            if (value == null || value.trim().isEmpty() || value.equals("")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 商品単価の算出
     * @param productList csvデータ
     * @return 結果情報 商品単価
     */
    private static Map<String, Double> getProductPrices(List<ProductData> productList) {
        Map<String, Double> productPrices = new HashMap<>();
        for (ProductData product : productList) {
            // 算出済みの商品は除外
            if (!productPrices.containsKey(product.getProductName())) {
                double price = product.getSales() / product.getQuantity();
                productPrices.put(product.getProductName(), price);
            }
        }
        return productPrices;
    }

    /**
     * 商品ごとの売上合計算出
     * @param productList csvデータ
     * @return 結果情報 商品ごとの売上
     */
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

    /**
     * 東京での月別最高売上商品
     * @param productList csvデータ
     * @return 結果情報 商品名
     */
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
        return bestProduct;
    }

    /**
     * 商品ごとの売上合計算出
     * @param resultExcelPath ファイルパス
     * @return 結果情報 算出結果Excelへの書き出し
     */
    private static void writeDataToExcel(Map<String, Double> productPrices,
                                         Map<String, Double> productSalesByType,
                                         String bestProductByMonth, String resultExcelPath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            writeSheet(workbook, "商品の種類と単価", productPrices);
            writeSheet(workbook, "商品の種類ごとの売上金額", productSalesByType);
            writeSingleCellSheet(workbook, "東京の売上が一番大きかった月の商品", bestProductByMonth);
            File outputDir = new File("result");
            outputDir.mkdirs();
            FileOutputStream fileOut = new FileOutputStream(resultExcelPath);
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