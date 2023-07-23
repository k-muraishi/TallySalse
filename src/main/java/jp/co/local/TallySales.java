package jp.co.local;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TallySales {

    private int id;
    private String productName;
    private Date salesDate;
    private int quantity;
    private int sales;
    private String store;

    private TallySales(int id, String productName, Date salesDate, int quantity, int sales, String store) {
        this.id = id;
        this.productName = productName;
        this.salesDate = salesDate;
        this.quantity = quantity;
        this.sales = sales;
        this.store = store;
    }

    public static void main(String[] args) {

        int columnsNum = 6;
        List<List<TallySales>> productList = new ArrayList<List<TallySales>>();
        List<List<TallySales>> dateList = new ArrayList<List<TallySales>>();

        try {
            File file = new File("sales.csv");
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = br.readLine();

            while(line != null){
                //データの修正
                if ((line.chars().filter(c -> c == ',').count()) > columnsNum-1) {

                    // データ修正メソッド呼び出し
                    fixData();


                } else {

                }


            }


        } catch (IOException e) {
            System.out.println("ファイル読み込みに失敗");
        }
    }

    public static void fixData() {
        // 商品名にカンマがある場合

        // 日付の後のカンマ数が2以上の場合
    }
}