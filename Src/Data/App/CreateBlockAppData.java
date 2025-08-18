package Data.App;

import Data.Constrcutor.AppData;
import Model.Block;
import Model.Transaction;
import Model.Wallet;
import Service.BlockchainService;
import Service.MinerService;
import Service.TransactionService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.PrintWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

public class CreateBlockAppData extends AppData {
    private int threadAmount;
    public boolean isGood;

    public CreateBlockAppData(JsonObject data, PrintWriter out) {
        super(data);
        this.threadAmount = data.get("threadAmount").getAsInt();
        JsonArray txIdArray = data.getAsJsonArray("ids");
        String publicKeyStr = data.get("publicKey").getAsString();
        String privateKeyStr = data.get("privateKey").getAsString();

        PrivateKey privateKey = TransactionService.getPrivateKey(privateKeyStr);
        PublicKey publicKey = TransactionService.getPublicKey(publicKeyStr);
        if (privateKey == null || publicKey == null) {
            this.isGood = false;
            return;
        }
        Wallet wallet = new Wallet(privateKey, publicKey);

        if (!wallet.validateWallet()) {
            System.out.println("Wallet is valid");
            this.isGood = false;
            return;
        }
        List<Transaction> transactions = new ArrayList<>();
        for (JsonElement el : txIdArray) {
            String txId = el.getAsString();
            transactions.add(TransactionService.getTransaction(txId));
            System.out.println("Transaction Id: " + txId + " added");
            System.out.println("Transaction: " + BlockchainService.gson.toJson(TransactionService.getTransaction(txId)));
        }

        Block block = BlockchainService.createNewBlock(transactions, wallet);
        MinerService.startMine(threadAmount, transactions, block, out);
        this.isGood = true;
    }

    public int getThreadAmount() { return threadAmount; }
}
